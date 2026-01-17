package com.example.backend.controller;

import com.example.backend.service.RedisHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 
 * Endpoints:
 * - GET /api/health/health - Basic health check
 * - GET /api/health/version - Version information
 * - GET /api/health/redis - Redis connection status
 * - GET /api/health/redis/test - Redis cache operations test
 * - GET /api/health/redis/stats - Redis cache statistics
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final RedisHealthService redisHealthService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "trading-platform-api");

        // Add Redis status
        boolean redisAvailable = redisHealthService.isRedisAvailable();
        response.put("redis", redisAvailable ? "UP" : "DOWN");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of(
                "version", "1.0.0-SNAPSHOT",
                "phase", "2",
                "description", "Phase 2: Database Optimization with Redis"));
    }

    /**
     * Redis connection health check
     * 
     * Returns:
     * - status: UP or DOWN
     * - ping: PONG if connected
     * - version, mode, os: Server information
     */
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> redisHealth() {
        Map<String, Object> info = redisHealthService.getRedisInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * Test Redis cache operations
     * 
     * Tests:
     * - SET with TTL
     * - GET
     * - DELETE
     * 
     * Returns test results for each operation
     */
    @GetMapping("/redis/test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> testResults = redisHealthService.testCacheOperations();
        return ResponseEntity.ok(testResults);
    }

    /**
     * Get Redis cache statistics
     * 
     * Returns:
     * - keyspace_hits: Number of successful lookups
     * - keyspace_misses: Number of failed lookups
     * - hit_rate_percent: Cache hit rate percentage
     */
    @GetMapping("/redis/stats")
    public ResponseEntity<Map<String, Object>> redisStats() {
        Map<String, Object> stats = redisHealthService.getCacheStats();
        return ResponseEntity.ok(stats);
    }
}
