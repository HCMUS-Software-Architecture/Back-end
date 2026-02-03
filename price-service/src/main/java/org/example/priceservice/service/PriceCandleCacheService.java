package org.example.priceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.priceservice.entity.PriceCandle;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCandleCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;

    private static final String CACHE_KEY_PREFIX = "candles:";

    /**
     * Tạo cache key dựa trên symbol, interval và limit
     */
    public String generateCacheKey(String symbol, String interval, int limit) {
        return CACHE_KEY_PREFIX + symbol.toUpperCase() + ":" + interval + ":" + limit;
    }

    /**
     * Lấy candles từ Redis cache
     * @return List candles nếu có trong cache, null nếu không có
     */
    @SuppressWarnings("unchecked")
    public List<PriceCandle> getFromCache(String symbol, String interval, int limit) {
        String cacheKey = generateCacheKey(symbol, interval, limit);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache HIT for key: {}", cacheKey);
                return (List<PriceCandle>) cached;
            }
            log.debug("Cache MISS for key: {}", cacheKey);
            return null;
        } catch (Exception e) {
            log.warn("Error reading from Redis cache for key {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * Lưu candles vào Redis cache với TTL
     */
    public void saveToCache(String symbol, String interval, int limit, List<PriceCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        String cacheKey = generateCacheKey(symbol, interval, limit);
        try {
            redisTemplate.opsForValue().set(cacheKey, candles, cacheTtl);
            log.debug("Saved {} candles to cache with key: {}, TTL: {}", candles.size(), cacheKey, cacheTtl);
        } catch (Exception e) {
            log.warn("Error saving to Redis cache for key {}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Xóa cache cho một symbol và interval cụ thể (invalidate khi có data mới)
     */
    public void invalidateCache(String symbol, String interval) {
        String pattern = CACHE_KEY_PREFIX + symbol.toUpperCase() + ":" + interval + ":*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Error invalidating cache for pattern {}: {}", pattern, e.getMessage());
        }
    }

    /**
     * Xóa toàn bộ cache candles
     */
    public void clearAllCache() {
        String pattern = CACHE_KEY_PREFIX + "*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys", keys.size());
            }
        } catch (Exception e) {
            log.warn("Error clearing all cache: {}", e.getMessage());
        }
    }
}
