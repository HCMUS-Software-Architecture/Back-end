package com.example.backend.service.candle;

import com.example.backend.dto.CandleDto;
import com.example.backend.entity.PriceCandle;
import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceCandleRepository;
import com.example.backend.service.TickBufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class PriceCandleService implements ICandleService {
    private final TickBufferService tickBufferService;
    private final PriceCandleRepository candleRepository;

    @Value("${price.symbols}")
    private String currencySymbols;

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final int[] supportedTime = { 1, 5, 10, 15, 60, 240 }; // 1m, 5m, 15m, 1h, 4h
    private final Map<String, PriceCandle> currentCandles = new ConcurrentHashMap<>();

    @Override
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
