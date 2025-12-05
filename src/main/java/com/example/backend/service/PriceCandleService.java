package com.example.backend.service;

import com.example.backend.entity.PriceCandle;
import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import com.example.backend.repository.PriceTickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleService {
    private final TickBufferService tickBufferService;
    private final PriceCandleRepository candleRepository;


    private final Map<String, PriceCandle> currentCandles = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 500)
    public void processBuffer() {
        Map<String, List<PriceTick>> batch = tickBufferService.drainBuffer();

        batch.forEach((symbol, ticks) -> {
            ticks.forEach(tick -> updateCurrentCandle1m(symbol, tick));
        });
    }

    private void updateCurrentCandle1m(String symbol, PriceTick tick) {
        Instant tickTime = tick.getTimestamp();
        Instant candleOpenTime = tickTime.truncatedTo(ChronoUnit.MINUTES);

        PriceCandle candle = currentCandles.get(symbol);

        // Trường hợp 1: Chưa có nến hoặc tick đã sang phút mới -> Lưu nến cũ, tạo nến mới
        if (candle == null || candle.getOpenTime().isBefore(candleOpenTime)) {
            if (candle != null) {
                // Đóng nến cũ hoàn toàn và lưu DB lần cuối
                saveCandle(candle);
            }
            // Tạo nến mới
            candle = PriceCandle.builder()
                    .symbol(symbol)
                    .interval("1m")
                    .openTime(candleOpenTime)
                    // Close time của nến 1m là open + 1 phút
                    .closeTime(candleOpenTime.plus(1, ChronoUnit.MINUTES))
                    .open(tick.getPrice())
                    .high(tick.getPrice())
                    .low(tick.getPrice())
                    .close(tick.getPrice())
                    .volume(tick.getQuantity())
                    .trades(1)
                    .build();
        } else {
            // Trường hợp 2: Tick vẫn nằm trong phút hiện tại -> Update High/Low/Close/Volume
            candle.setClose(tick.getPrice());
            if (tick.getPrice().compareTo(candle.getHigh()) > 0) candle.setHigh(tick.getPrice());
            if (tick.getPrice().compareTo(candle.getLow()) < 0) candle.setLow(tick.getPrice());
            candle.setVolume(candle.getVolume().add(tick.getQuantity()));
            candle.setTrades(candle.getTrades() + 1);
        }

        currentCandles.put(symbol, candle);

        // Tùy chọn: Có thể save() liên tục mỗi 1s nếu muốn DB update realtime (hơi tốn resource)
        // Hoặc chỉ save khi đóng nến (như code trên dòng 48).
        // Để an toàn, mình khuyên save mỗi lần update (upsert) nếu lượng user ít.

    }

    private void saveCandle(PriceCandle candle) {
        try {
            candleRepository.save(candle);
        } catch (Exception e) {
            log.error("Error saving candle: {}", e.getMessage());
        }
    }

    // 2. JOB TẠO NẾN 5M (Rollup từ nến 1m) - Thay thế aggregate5mCandles cũ
    @Scheduled(fixedRate = 300000, initialDelay = 300000) // 5m
    public void aggregate5mCandles() {
        rollupCandles("5m", 5);
    }

    // 3. Candle 10m
    @Scheduled(fixedRate = 600000, initialDelay = 600000)
    public void aggregate10mCandles() {
        rollupCandles("10m", 10);
    }

    // 3. JOB TẠO NẾN 1H (Rollup từ nến 1m hoặc 5m) - Thay thế aggregate1hCandles cũ
    @Scheduled(fixedRate = 3601000) // 1 hour 10s from app starting
    public void aggregate1hCandles() {
        rollupCandles("1h", 60);
    }

    // Hàm chung để gộp nến nhỏ thành nến lớn
    private void rollupCandles(String targetInterval, int minutes) {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT"); // Nên lấy từ config
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant startTime = endTime.minus(minutes, ChronoUnit.MINUTES);

        for (String symbol : symbols) {
            // Lấy các nến 1m trong khoảng thời gian này
            List<PriceCandle> sourceCandles = candleRepository.findBySymbolAndIntervalAndOpenTimeBetween(
                    symbol, "1m", startTime, endTime.minusSeconds(1) // Trừ 1s để không lấy nến của phút hiện tại
            );

            if (sourceCandles.isEmpty()) continue;

            // Tính toán gộp
            BigDecimal open = sourceCandles.get(0).getOpen();
            BigDecimal close = sourceCandles.get(sourceCandles.size() - 1).getClose();
            BigDecimal high = sourceCandles.stream().map(PriceCandle::getHigh).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = sourceCandles.stream().map(PriceCandle::getLow).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal volume = sourceCandles.stream().map(PriceCandle::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add);
            int trades = sourceCandles.stream().mapToInt(PriceCandle::getTrades).sum();

            PriceCandle bigCandle = PriceCandle.builder()
                    .symbol(symbol)
                    .interval(targetInterval)
                    .openTime(startTime)
                    .closeTime(endTime)
                    .open(open)
                    .close(close)
                    .high(high)
                    .low(low)
                    .volume(volume)
                    .trades(trades)
                    .build();

            candleRepository.save(bigCandle);
            log.info("Generated {} candle for {}", targetInterval, symbol);
        }
    }

    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        return candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(
                symbol.toUpperCase(),
                interval,
                PageRequest.of(0, limit)
        );
    }
}
