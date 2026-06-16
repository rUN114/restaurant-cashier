package com.restaurant.cashiersystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * 嵌入式 Redis：项目启动时自动在 localhost:6379 拉起 Redis，
 * 无需手动执行 redis-server。如果端口已被占用（外部 Redis 已运行）则跳过。
 */
@Configuration
@ConditionalOnProperty(name = "embedded.redis.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedRedisConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedisConfig.class);

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startEmbeddedRedis() {
        if (isPortInUse(redisPort)) {
            log.info("端口 {} 已被占用，跳过嵌入式 Redis 启动（可能外部 Redis 已在运行）", redisPort);
            return;
        }
        try {
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            log.info("嵌入式 Redis 已在 localhost:{} 启动成功", redisPort);
        } catch (IOException e) {
            log.warn("嵌入式 Redis 启动失败: {}。将使用 CacheErrorHandler 降级处理。", e.getMessage());
        }
    }

    @PreDestroy
    public void stopEmbeddedRedis() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
                log.info("嵌入式 Redis 已停止");
            } catch (IOException e) {
                log.warn("嵌入式 Redis 停止异常: {}", e.getMessage());
            }
        }
    }

    private boolean isPortInUse(int port) {
        try (java.net.Socket socket = new java.net.Socket("localhost", port)) {
            return true; // 能连上说明端口在用
        } catch (IOException e) {
            return false;
        }
    }
}
