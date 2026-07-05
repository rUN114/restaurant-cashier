package com.restaurant.cashiersystem.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @PostConstruct
    private void validateSecret() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT 密钥未配置或长度不足（至少32字符）。请在环境变量/配置文件中设置 jwt.secret。" +
                " 示例: export JWT_SECRET=$(openssl rand -base64 48)");
        }
        log.info("JWT 密钥已加载，长度: {} 字符", secret.length());
    }

    /**
     * 生成Token（包含角色和门店信息）
     * @param userId   用户ID
     * @param username 用户名
     * @param roleCode 角色编码（如 admin_global）
     * @param storeId  门店ID（可为 null）
     */
    public String generateToken(Long userId, String username, String roleCode, Long storeId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("roleCode", roleCode);          // 新增
        if (storeId != null) {
            claims.put("storeId", storeId);        // 新增
        }
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}