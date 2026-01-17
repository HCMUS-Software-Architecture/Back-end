package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Health Check Service
 * 
 * Purpose: Test Redis connection and provide health status
 * 
 * Methods:
 * - isRedisAvailable(): Simple ping test
 * - getRedisInfo(): Detailed connection information
 * - testCacheOperations(): Test set/get/delete operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisHealthService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if Redis is available
     * 
     * @return true if Redis responds to PING, false otherwise
     */
    public boolean isRedisAvailable() {
        try {
            String response = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            log.info("Redis PING response: {}", response);
            return "PONG".equalsIgnoreCase(response);
        } catch (Exception e) {
            log.error("Redis connection failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Redis connection information
     * 
     * @return Map containing connection details and statistics
     */
    public Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

            // Basic connection test
            String pingResponse = connection.ping();
            info.put("status", "UP");
            info.put("ping", pingResponse);

            // Get server info
            try {
                var serverInfo = connection.serverCommands().info();
                info.put("version", serverInfo.getProperty("redis_version"));
                info.put("mode", serverInfo.getProperty("redis_mode"));
                info.put("os", serverInfo.getProperty("os"));
                info.put("uptime_days", serverInfo.getProperty("uptime_in_days"));
            } catch (Exception e) {
                log.warn("Could not fetch detailed server info: {}", e.getMessage());
            }

            // Connection pool info
            info.put("database", "0"); // Default database

            connection.close();
        } catch (Exception e) {
            log.error("Failed to get Redis info: {}", e.getMessage());
            info.put("status", "DOWN");
            info.put("error", e.getMessage());
        }

        return info;
    }

    /**
     * Test basic cache operations
     * 
     * Tests:
     * 1. SET key with TTL
     * 2. GET key
     * 3. DELETE key
     * 
     * @return Map with test results
     */
    public Map<String, Object> testCacheOperations() {
        Map<String, Object> results = new HashMap<>();
        String testKey = "health:test:" + System.currentTimeMillis();
        String testValue = "Redis connection test - " + System.currentTimeMillis();

        try {
            // Test 1: SET with TTL
            redisTemplate.opsForValue().set(testKey, testValue, Duration.ofSeconds(10));
            results.put("set", "SUCCESS");
            log.debug("SET test successful: {} = {}", testKey, testValue);

            // Test 2: GET
            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
            if (testValue.equals(retrievedValue)) {
                results.put("get", "SUCCESS");
                log.debug("GET test successful: {} = {}", testKey, retrievedValue);
            } else {
                results.put("get", "FAILED - Value mismatch");
                log.warn("GET test failed: expected {}, got {}", testValue, retrievedValue);
            }

            // Test 3: DELETE
            Boolean deleted = redisTemplate.delete(testKey);
            results.put("delete", deleted ? "SUCCESS" : "FAILED");
            log.debug("DELETE test: {}", deleted ? "SUCCESS" : "FAILED");

            // Overall status
            results.put("status", "ALL_TESTS_PASSED");
            results.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Cache operation test failed: {}", e.getMessage());
            results.put("status", "FAILED");
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * Get cache statistics
     * 
     * @return Map with cache hit/miss information (if available)
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            var info = connection.serverCommands().info("stats");

            stats.put("keyspace_hits", info.getProperty("keyspace_hits", "N/A"));
            stats.put("keyspace_misses", info.getProperty("keyspace_misses", "N/A"));
            stats.put("total_commands_processed", info.getProperty("total_commands_processed", "N/A"));

            // Calculate hit rate
            try {
                long hits = Long.parseLong(info.getProperty("keyspace_hits", "0"));
                long misses = Long.parseLong(info.getProperty("keyspace_misses", "0"));
                double total = hits + misses;
                double hitRate = total > 0 ? (hits / total) * 100 : 0;
                stats.put("hit_rate_percent", String.format("%.2f%%", hitRate));
            } catch (Exception e) {
                log.warn("Could not calculate hit rate: {}", e.getMessage());
            }

            connection.close();
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
