package com.restaurant.cashiersystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录频率限制 — Redis 固定窗口计数器
 * <ul>
 *   <li>IP 维度：15 分钟内最多 10 次失败，超限锁定 30 分钟</li>
 *   <li>账号维度：15 分钟内最多 5 次失败，超限锁定 30 分钟</li>
 *   <li>Redis 不可用时自动降级放行，不阻塞正常登录</li>
 * </ul>
 */
@Service
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    /** IP 窗口内最大失败次数 */
    private static final int IP_MAX_ATTEMPTS = 10;
    /** 账号窗口内最大失败次数 */
    private static final int USER_MAX_ATTEMPTS = 5;
    /** 计数窗口（分钟） */
    private static final int WINDOW_MINUTES = 15;
    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** Lua 脚本：原子 INCR + EXPIRE + 阈值检查 */
    private final DefaultRedisScript<Long> rateLimitScript;

    public LoginRateLimiter() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end " +
            "if count >= tonumber(ARGV[2]) then redis.call('SET', KEYS[2], '1', 'EX', ARGV[3]) end " +
            "return count");
        rateLimitScript.setResultType(Long.class);
    }

    // ==================== Redis Key ====================

    private static String ipFailKey(String ip) {
        return "login:fail:ip:" + ip;
    }

    private static String ipLockKey(String ip) {
        return "login:lock:ip:" + ip;
    }

    private static String userFailKey(String username) {
        return "login:fail:user:" + username;
    }

    private static String userLockKey(String username) {
        return "login:lock:user:" + username;
    }

    // ==================== 公共 API ====================

    /**
     * 登录前检查：是否被限流。
     * @return 限流原因（null = 未被限流，可以继续登录）
     */
    public String checkBeforeLogin(String username, String ip) {
        try {
            // 先检查账号级锁（更精细的锁定）
            String userLockVal = redisTemplate.opsForValue().get(userLockKey(username));
            if (userLockVal != null) {
                Long remaining = redisTemplate.getExpire(userLockKey(username), TimeUnit.SECONDS);
                long minutes = (remaining != null && remaining > 0) ? remaining / 60 : LOCK_MINUTES;
                log.warn("账号 {} 登录被限流拦截（账号锁定，剩余 {} 分钟）", username, minutes);
                return "该账号登录尝试过于频繁，请 " + minutes + " 分钟后再试";
            }

            // 再检查 IP 级锁
            String ipLockVal = redisTemplate.opsForValue().get(ipLockKey(ip));
            if (ipLockVal != null) {
                Long remaining = redisTemplate.getExpire(ipLockKey(ip), TimeUnit.SECONDS);
                long minutes = (remaining != null && remaining > 0) ? remaining / 60 : LOCK_MINUTES;
                log.warn("IP {} 登录被限流拦截（IP 锁定，剩余 {} 分钟）", ip, minutes);
                return "当前网络登录尝试过于频繁，请 " + minutes + " 分钟后再试";
            }
            return null;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过登录限流检查", e);
            return null; // Redis 挂了不阻塞登录
        }
    }

    /**
     * 登录失败后调用，记录失败并检查是否需要锁定。
     */
    public void recordFailedAttempt(String username, String ip) {
        try {
            // IP 维度
            long ipCount = incrAndCheck(ipFailKey(ip), IP_MAX_ATTEMPTS, ipLockKey(ip));
            if (ipCount >= IP_MAX_ATTEMPTS) {
                log.warn("IP {} 15分钟内失败 {} 次，已锁定 {} 分钟", ip, ipCount, LOCK_MINUTES);
            }

            // 账号维度
            long userCount = incrAndCheck(userFailKey(username), USER_MAX_ATTEMPTS, userLockKey(username));
            if (userCount >= USER_MAX_ATTEMPTS) {
                log.warn("账号 {} 15分钟内失败 {} 次，已锁定 {} 分钟", username, userCount, LOCK_MINUTES);
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过登录失败记录", e);
        }
    }

    /**
     * 登录成功后调用，清除失败记录。
     */
    public void clearAttempts(String username, String ip) {
        try {
            redisTemplate.delete(ipFailKey(ip));
            redisTemplate.delete(ipLockKey(ip));
            redisTemplate.delete(userFailKey(username));
            redisTemplate.delete(userLockKey(username));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过清除登录记录", e);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 对计数器 +1，Lua 脚本保证 INCR+EXPIRE+阈值检查原子性
     */
    private long incrAndCheck(String failKey, int maxAttempts, String lockKey) {
        Long count = redisTemplate.execute(
                rateLimitScript,
                List.of(failKey, lockKey),
                String.valueOf(WINDOW_MINUTES * 60),
                String.valueOf(maxAttempts),
                String.valueOf(LOCK_MINUTES * 60));
        return count != null ? count : 0;
    }
}
