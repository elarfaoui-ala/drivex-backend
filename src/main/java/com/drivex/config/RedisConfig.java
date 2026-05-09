package com.drivex.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {

    /**
     * Jackson ObjectMapper that handles Java 8 time types (LocalDateTime, etc.)
     * — used for Redis JSON serialization.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * RedisTemplate — stores all values as JSON strings.
     * Keys are plain strings.
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory factory,
        ObjectMapper redisObjectMapper
    ) {
        var template   = new RedisTemplate<String, Object>();
        var jsonSerial = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerial);
        template.setHashValueSerializer(jsonSerial);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * RedisCacheManager — used by @Cacheable / @CacheEvict annotations.
     *
     * Cache TTLs:
     *   driver:profile  →  5 min
     *   drivers:online  →  30 sec  (changes frequently)
     *   orders:*        →  1 min
     *   earnings:*      →  2 min
     */
    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager cacheManager(
        RedisConnectionFactory factory,
        ObjectMapper redisObjectMapper
    ) {
        var jsonSerial = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        var valueSerializer = RedisSerializationContext.SerializationPair
            .fromSerializer(jsonSerial);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(valueSerializer)
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaults)
            .withCacheConfiguration("driver:profile",
                defaults.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("drivers:online",
                defaults.entryTtl(Duration.ofSeconds(30)))
            .withCacheConfiguration("orders:available",
                defaults.entryTtl(Duration.ofMinutes(1)))
            .withCacheConfiguration("order:detail",
                defaults.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("earnings:today",
                defaults.entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("earnings:week",
                defaults.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("earnings:month",
                defaults.entryTtl(Duration.ofMinutes(10)))
            .build();
    }

    /**
     * Fallback CacheManager — used automatically when Redis is unreachable.
     * Keeps the app running with in-memory cache; log a warning.
     *
     * To activate: set spring.cache.type=simple in application.yml
     * or let the app fail-over automatically via try-catch at startup.
     */
    @Bean("fallbackCacheManager")
    public CacheManager fallbackCacheManager() {
        log.warn("Using in-memory fallback CacheManager — Redis is not available");
        return new ConcurrentMapCacheManager(
            "driver:profile", "drivers:online",
            "orders:available", "order:detail",
            "earnings:today", "earnings:week", "earnings:month"
        );
    }
}
