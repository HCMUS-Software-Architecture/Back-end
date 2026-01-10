package com.example.backend;

import com.example.backend.service.RedisHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Integration Test
 * 
 * Tests:
 * 1. Redis connectivity
 * 2. SET/GET operations via RedisTemplate
 * 3. Cache operations via RedisHealthService
 * 4. Server information retrieval
 */
@SpringBootTest
public class RedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisHealthService redisHealthService;

    @Test
    public void testRedisAvailability() {
        System.out.println("\n=== Test 1: Redis Availability ===");
        boolean available = redisHealthService.isRedisAvailable();
        System.out.println("Redis Available: " + available);
        assertTrue(available, "Redis should be available");
    }

    @Test
    public void testRedisInfo() {
        System.out.println("\n=== Test 2: Redis Server Info ===");
        Map<String, Object> info = redisHealthService.getRedisInfo();
        System.out.println("Redis Info: " + info);

        assertNotNull(info);
        assertEquals("UP", info.get("status"));
        assertEquals("PONG", info.get("ping"));
    }

    @Test
    public void testCacheOperations() {
        System.out.println("\n=== Test 3: Cache Operations ===");
        Map<String, Object> results = redisHealthService.testCacheOperations();
        System.out.println("Test Results: " + results);

        assertNotNull(results);
        assertEquals("SUCCESS", results.get("set"));
        assertEquals("SUCCESS", results.get("get"));
        assertEquals("SUCCESS", results.get("delete"));
        assertEquals("ALL_TESTS_PASSED", results.get("status"));
    }

    @Test
    public void testRedisTemplateDirectly() {
        System.out.println("\n=== Test 4: RedisTemplate Direct Usage ===");

        String testKey = "integration:test:" + System.currentTimeMillis();
        String testValue = "Direct Redis Template Test";

        // SET
        redisTemplate.opsForValue().set(testKey, testValue);
        System.out.println("SET: " + testKey + " = " + testValue);

        // GET
        Object retrieved = redisTemplate.opsForValue().get(testKey);
        System.out.println("GET: " + testKey + " = " + retrieved);
        assertEquals(testValue, retrieved);

        // DELETE
        Boolean deleted = redisTemplate.delete(testKey);
        System.out.println("DELETE: " + testKey + " = " + deleted);
        assertTrue(deleted);

        // Verify deletion
        Object shouldBeNull = redisTemplate.opsForValue().get(testKey);
        System.out.println("After DELETE: " + testKey + " = " + shouldBeNull);
        assertNull(shouldBeNull);
    }

    @Test
    public void testRedisCacheStats() {
        System.out.println("\n=== Test 5: Redis Cache Statistics ===");
        Map<String, Object> stats = redisHealthService.getCacheStats();
        System.out.println("Cache Stats: " + stats);

        assertNotNull(stats);
        // Stats may vary, just check it doesn't error
    }

    @Test
    public void testRedisConnectionFactory() {
        System.out.println("\n=== Test 6: Connection Factory ===");

        assertNotNull(redisTemplate.getConnectionFactory(),
                "Connection Factory should not be null");

        var connection = redisTemplate.getConnectionFactory().getConnection();
        assertNotNull(connection, "Connection should not be null");

        String ping = connection.ping();
        System.out.println("PING Response: " + ping);
        assertEquals("PONG", ping);

        connection.close();
    }
}
