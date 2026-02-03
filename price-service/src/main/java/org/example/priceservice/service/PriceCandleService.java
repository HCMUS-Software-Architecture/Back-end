package org.example.priceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.priceservice.entity.PriceCandle;
import org.example.priceservice.repository.PriceCandleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceCandleService {
    private final PriceCandleRepository priceCandleRepository;
    private final PriceCandleCacheService cacheService;

    public List<PriceCandle> getCandles(String symbol, String interval, int limit) {
        String normalizedSymbol = symbol.toUpperCase();

        log.info("Fetching candles for symbol={}, interval={}, limit={}", normalizedSymbol, interval, limit);

        // 1. Kiểm tra Redis cache trước
        List<PriceCandle> cachedCandles = cacheService.getFromCache(normalizedSymbol, interval, limit);
        if (cachedCandles != null && !cachedCandles.isEmpty()) {
            log.info("Returning {} candles from Redis cache", cachedCandles.size());
            return cachedCandles;
        }

        // 2. Cache miss -> Query từ MongoDB
        log.info("Cache miss, querying from MongoDB");
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "openTime"));

        List<PriceCandle> candles = priceCandleRepository.findBySymbolAndInterval(normalizedSymbol, interval, pageable);

        log.info("Found {} candles in database", candles.size());

        if (!candles.isEmpty()) {
            log.debug("First candle time: {}, Last candle time: {}",
                    candles.get(0).getOpenTime(),
                    candles.get(candles.size() - 1).getOpenTime());

            // Reverse để trả về theo thứ tự thời gian tăng dần (cũ -> mới) cho FE
            Collections.reverse(candles);

            // 3. Lưu vào Redis cache cho lần request tiếp theo
            cacheService.saveToCache(normalizedSymbol, interval, limit, candles);
        }

        return candles;
    }
}
