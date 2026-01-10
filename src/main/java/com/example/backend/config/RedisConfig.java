package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration for Caching
 * 
 * Purpose: Configure Redis as a caching layer to reduce database load
 * Reference: Phase2-ImplementationGuide.md - Redis Caching section
 * 
 * Key Features:
 * - Cache-aside pattern for frequently accessed data
 * - JSON serialization for complex objects
 * - Configurable TTL (Time To Live) for cache entries
 * - Support for multiple cache namespaces
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Configure RedisTemplate for manual cache operations
     * 
     * Used for:
     * - Direct Redis operations (get, set, delete)
     * - Custom caching logic beyond @Cacheable
     * - Real-time data broadcasting
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Configuring RedisTemplate with JSON serialization");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure CacheManager for Spring's @Cacheable annotations
     * 
     * Default TTL: 10 minutes (600 seconds)
     * Override per cache: Use @Cacheable(cacheNames = "customCache")
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring RedisCacheManager with 10-minute default TTL");

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                createJsonSerializer()))
                .disableCachingNullValues(); // Don't cache null values

        // Create cache-specific configurations
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                // Articles cache: 5 minutes TTL (articles change frequently)
                .withCacheConfiguration("articles",
                        cacheConfig.entryTtl(Duration.ofMinutes(5)))
                // Article by ID: 15 minutes TTL (individual articles rarely change)
                .withCacheConfiguration("article",
                        cacheConfig.entryTtl(Duration.ofMinutes(15)))
                // Price candles: 1 minute TTL (price data changes rapidly)
                .withCacheConfiguration("candles",
                        cacheConfig.entryTtl(Duration.ofMinutes(1)))
                // Latest candle: 30 seconds TTL (real-time data)
                .withCacheConfiguration("latestCandle",
                        cacheConfig.entryTtl(Duration.ofSeconds(30)))
                .build();
    }

    /**
     * Create JSON serializer with type information
     * 
     * Why type information?
     * - Allows deserializing to correct Java types (Article, PriceCandle, etc.)
     * - Handles LocalDateTime, BigDecimal, and other Java types
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register Java 8 time module (LocalDateTime, Instant, etc.)
        objectMapper.registerModule(new JavaTimeModule());

        // Enable type information for polymorphic deserialization
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
