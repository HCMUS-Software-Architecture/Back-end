package com.example.backend.service;

import com.example.backend.dto.CandleDto;
import com.example.backend.entity.PriceCandle;
import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Price Candle Service with Redis Caching
 * 
 * Cache Strategy:
 * - getCandles: Cache historical candles for 1 minute
 * - saveCandle: Evict cache when new candle is saved
 * 
 * Why short TTL?
 * - Price data changes rapidly (every 500ms)
 * - Cache is mainly for historical data, not real-time updates
 * - Real-time updates use WebSocket, not cache
 * 
 * Reference: Phase2-ImplementationGuide.md - Price Collector Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleService {
    private final TickBufferService tickBufferService;
    private final PriceCandleRepository candleRepository;

    @Value("${price.symbols}")
    private String currencySymbols;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final int[] supportedTime = { 1, 5, 10, 15, 60, 240 }; // 1m, 5m, 15m, 1h, 4h
    private final Map<String, PriceCandle> currentCandles = new ConcurrentHashMap<>();

    private void broadcastCurrentCandle(String symbol, String timeCode) {
        String key = symbol + "_" + timeCode;
        PriceCandle priceCandle = currentCandles.get(key);

        if (priceCandle == null) {
            return;
        }

        CandleDto candleDto = new CandleDto();
        candleDto.setSymbol(symbol);
        candleDto.setOpen(priceCandle.getOpen());
        candleDto.setHigh(priceCandle.getHigh());
        candleDto.setLow(priceCandle.getLow());
        candleDto.setClose(priceCandle.getClose());
        candleDto.setVolume(priceCandle.getVolume());
        candleDto.setOpenTime(priceCandle.getOpenTime().toEpochMilli());

        // log.info(">>> ĐANG BẮN SOCKET {} cho {}: Giá = {}",
        // timeCode, symbol, candleDto.getClose());

        simpMessagingTemplate.convertAndSend("/topic/candles/" + timeCode + "/" + symbol.toLowerCase(), candleDto);
    }

    @Scheduled(fixedRate = 500)
    public void processBuffer() {
        try {
            Map<String, List<PriceTick>> batch = tickBufferService.drainBuffer();

            batch.forEach((symbol, ticks) -> {
                for (int time : supportedTime) {
                    String timeCode = time + "m";
                    ticks.forEach(tick -> updateCurrentCandleState(symbol, time, tick));
                    broadcastCurrentCandle(symbol, timeCode);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void updateCurrentCandleState(String symbol, int timeMinute, PriceTick tick) {
        long tickMillis = tick.getTimestamp().toEpochMilli();
        long intervalMillis = timeMinute * 60 * 1000L;
        long openTimeMillis = (tickMillis / intervalMillis) * intervalMillis;
        Instant openTime = Instant.ofEpochMilli(openTimeMillis);
        String key = symbol + "_" + timeMinute + "m";

        currentCandles.compute(key, (k, candle) -> {
            if (candle == null || !candle.getOpenTime().equals(openTime)) {
                if (candle != null) {
                    // Đóng nến cũ hoàn toàn và lưu DB lần cuối
                    saveCandle(candle);
                }
                // Tạo nến mới
                candle = PriceCandle.builder()
                        .symbol(symbol)
                        .interval(timeMinute + "m")
                        .openTime(openTime)
                        .closeTime(openTime.plus(timeMinute, ChronoUnit.MINUTES))
                        .open(tick.getPrice())
                        .high(tick.getPrice())
                        .low(tick.getPrice())
                        .close(tick.getPrice())
                        .volume(tick.getQuantity())
                        .trades(1)
                        .createdAt(LocalDateTime.now())
                        .build();
            } else {
                // Trường hợp 2: Tick vẫn nằm trong phút hiện tại -> Update
                // High/Low/Close/Volume
                candle.setClose(tick.getPrice());
                if (tick.getPrice().compareTo(candle.getHigh()) > 0)
                    candle.setHigh(tick.getPrice());
                if (tick.getPrice().compareTo(candle.getLow()) < 0)
                    candle.setLow(tick.getPrice());
                candle.setVolume(candle.getVolume().add(tick.getQuantity()));
                candle.setTrades(candle.getTrades() + 1);
            }
            return candle;
        });

    }

    /**
     * Save candle to database and evict cache
     * 
     * Cache Eviction:
     * - Evicts "candles" cache for this symbol+interval combination
     * - Ensures next getCandles() call fetches fresh data
     */
    @CacheEvict(value = "candles", key = "#candle.symbol + '_' + #candle.interval")
    public void saveCandle(PriceCandle candle) {
        try {
            log.info("Saving candle {} to DB - Interval: {}", candle.getSymbol(), candle.getInterval());
            candleRepository.save(candle);
        } catch (Exception e) {
            log.error("Error saving candle: {}", e.getMessage());
        }
    }

    /**
     * Get historical candles with caching
     * 
     * Cache: "candles" (1-minute TTL)
     * Key: symbol + interval + limit (e.g., "candles::BTCUSDT_1m_100")
     * 
     * Why cache?
     * - Reduces PostgreSQL load for chart initial load
     * - Historical data doesn't change frequently
     * - Real-time updates use WebSocket, not this method
     */
    @Cacheable(value = "candles", key = "#symbol + '_' + #interval + '_' + #limit")
    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        log.debug("Fetching candles from database - Symbol: {}, Interval: {}, Limit: {}",
                symbol, interval, limit);
        return candleRepository.findBySymbolAndIntervalOrderByOpenTimeDesc(
                symbol.toUpperCase(),
                interval,
                PageRequest.of(0, limit));
    }

    /**
     * Delete old candles and evict cache
     * 
     * Scheduled: Every hour (0 0 * * * *)
     * Cache Eviction: Clear all candles cache after deletion
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @CacheEvict(value = "candles", allEntries = true)
    public void deletePriceCandlePeriodic() {
        LocalDateTime now = LocalDateTime.now().minusWeeks(1);
        log.info("Deleting candles older than {} and evicting cache", now);
        candleRepository.deleteByCreatedAtBeforeOrCreatedAtIsNull(now);
    }
}
