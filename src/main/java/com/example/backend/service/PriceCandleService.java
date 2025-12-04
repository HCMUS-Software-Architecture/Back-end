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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleService {
    private final PriceTickRepository tickRepository;
    private final PriceCandleRepository candleRepository;

    // Aggregate every minute for 1m candles
    @Scheduled(cron = "0 * * * * *")
    public void aggregate1mCandles() {
        aggregateCandles("1m", 1);
    }

    // Aggregate every 5 minutes for 5m candles
    @Scheduled(cron = "0 */5 * * * *")
    public void aggregate5mCandles() {
        aggregateCandles("5m", 5);
    }

    // Aggregate every hour for 1h candles
    @Scheduled(cron = "0 0 * * * *")
    public void aggregate1hCandles() {
        aggregateCandles("1h", 60);
    }

    private void aggregateCandles(String interval, int minutes) {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT");

        for (String symbol : symbols) {
            try {
                Instant end = Instant.now().truncatedTo(ChronoUnit.MINUTES);
                Instant start = end.minus(minutes, ChronoUnit.MINUTES);

                List<PriceTick> ticks = tickRepository.findBySymbolAndTimestampBetween(
                        symbol, start, end
                );

                if (ticks.isEmpty()) continue;

                PriceCandle candle = PriceCandle.builder()
                        .symbol(symbol)
                        .interval(interval)
                        .openTime(start)
                        .closeTime(end)
                        .open(ticks.get(0).getPrice())
                        .close(ticks.get(ticks.size() - 1).getPrice())
                        .high(ticks.stream()
                                .map(PriceTick::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO))
                        .low(ticks.stream()
                                .map(PriceTick::getPrice)
                                .min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO))
                        .volume(ticks.stream()
                                .map(PriceTick::getQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .trades(ticks.size())
                        .build();

                candleRepository.save(candle);
                log.debug("Saved {} candle for {}", interval, symbol);

            } catch (Exception e) {
                log.error("Failed to aggregate {} candle for {}: {}", interval, symbol, e.getMessage());
            }
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
