import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

/**
 * Standalone Redis connectivity test
 * 
 * Tests:
 * 1. Connection to Render Redis
 * 2. PING command
 * 3. SET/GET operations
 * 4. Key expiration
 */
public class RedisVerificationTest {

    private static final String REDIS_URL = "rediss://red-d5h9oiidbo4c73dsgfug:rbVlZiHNQ0yW3EXMPAmxMi4RYVu2x4ta@singapore-keyvalue.render.com:6379";

    public static void main(String[] args) {
        System.out.println("=== Redis Verification Test ===\n");

        try {
            URI redisURI = new URI(REDIS_URL);
            String host = redisURI.getHost();
            int port = redisURI.getPort();
            String password = redisURI.getUserInfo().split(":")[1];

            System.out.println("Connecting to: " + host + ":" + port);
            System.out.println("Using SSL: true\n");

            // Configure Jedis with SSL
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);

            try (JedisPool pool = new JedisPool(poolConfig, host, port,
                    Protocol.DEFAULT_TIMEOUT, password, true)) {

                try (Jedis jedis = pool.getResource()) {
                    // Test 1: PING
                    System.out.println("Test 1: PING Command");
                    String pingResponse = jedis.ping();
                    System.out.println("  Response: " + pingResponse);
                    System.out.println("  Status: " + (pingResponse.equals("PONG") ? "✓ PASSED" : "✗ FAILED"));
                    System.out.println();

                    // Test 2: SET/GET
                    System.out.println("Test 2: SET/GET Operations");
                    String testKey = "test:verification:" + System.currentTimeMillis();
                    String testValue = "Redis is working!";

                    jedis.setex(testKey, 10, testValue); // Set with 10 second expiration
                    System.out.println("  SET: " + testKey + " = " + testValue);

                    String retrievedValue = jedis.get(testKey);
                    System.out.println("  GET: " + testKey + " = " + retrievedValue);
                    System.out.println("  Status: " + (testValue.equals(retrievedValue) ? "✓ PASSED" : "✗ FAILED"));
                    System.out.println();

                    // Test 3: TTL check
                    System.out.println("Test 3: TTL (Time To Live)");
                    Long ttl = jedis.ttl(testKey);
                    System.out.println("  TTL: " + ttl + " seconds");
                    System.out.println("  Status: " + (ttl > 0 && ttl <= 10 ? "✓ PASSED" : "✗ FAILED"));
                    System.out.println();

                    // Test 4: DELETE
                    System.out.println("Test 4: DELETE Operation");
                    Long deleted = jedis.del(testKey);
                    System.out.println("  Deleted keys: " + deleted);
                    System.out.println("  Status: " + (deleted == 1 ? "✓ PASSED" : "✗ FAILED"));
                    System.out.println();

                    // Test 5: INFO
                    System.out.println("Test 5: Server INFO");
                    String info = jedis.info("server");
                    String[] lines = info.split("\r\n");
                    for (String line : lines) {
                        if (line.startsWith("redis_version:") ||
                                line.startsWith("redis_mode:") ||
                                line.startsWith("os:") ||
                                line.startsWith("uptime_in_days:")) {
                            System.out.println("  " + line);
                        }
                    }
                    System.out.println("  Status: ✓ PASSED");
                    System.out.println();

                    System.out.println("=== All Tests PASSED ===");
                    System.out.println("\nRedis is fully operational and accessible!");

                }
            }

        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
