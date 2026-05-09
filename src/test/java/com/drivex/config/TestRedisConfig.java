package com.drivex.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides mock Redis beans for tests when Redis auto-configuration is disabled.
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean("redisTemplate")
    @Primary
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper redisObjectMapper) {
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);

        when(template.opsForValue()).thenReturn(valueOps);
        when(template.opsForSet()).thenReturn(mock(SetOperations.class));
        when(template.opsForList()).thenReturn(mock(ListOperations.class));
        when(template.opsForZSet()).thenReturn(mock(ZSetOperations.class));
        when(template.opsForHash()).thenReturn(mock(HashOperations.class));
        when(template.opsForGeo()).thenReturn(mock(GeoOperations.class));
        when(template.opsForHyperLogLog()).thenReturn(mock(HyperLogLogOperations.class));

        return template;
    }
}
