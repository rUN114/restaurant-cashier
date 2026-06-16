package com.restaurant.cashiersystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * 创建支持 Java 8 时间类型 + 类型信息的 Jackson ObjectMapper。
     * 必须启用 default typing，否则反序列化时 Result 会变成 LinkedHashMap。
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 启用默认类型信息 — 等同于无参 GenericJackson2JsonRedisSerializer() 的行为
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    /**
     * 创建支持 LocalDateTime 的 JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        return new GenericJackson2JsonRedisSerializer(createObjectMapper());
    }

    /**
     * 自定义 RedisTemplate：Key 用 String，Value 用 JSON
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(createJsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(createJsonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 自定义 CacheManager：默认 TTL 30 分钟，支持按缓存名设置不同 TTL。
     * 使用 cachePrefix 隔离新旧缓存，避免旧格式 LinkedHashMap 数据导致 ClassCastException。
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("v2:")  // ← 隔离旧缓存，旧 key 自然过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("stores",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("roles",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("configs",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .build();
    }

    /**
     * Redis 不可用时降级处理：记录警告日志，让请求穿透到数据库，
     * 而不是抛出 RedisConnectionFailureException 导致接口 500。
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Redis GET 失败, cache={}, key={}: {}", cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Redis PUT 失败, cache={}, key={}: {}", cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Redis EVICT 失败, cache={}, key={}: {}", cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Redis CLEAR 失败, cache={}: {}", cache.getName(), ex.getMessage());
            }
        };
    }
}
