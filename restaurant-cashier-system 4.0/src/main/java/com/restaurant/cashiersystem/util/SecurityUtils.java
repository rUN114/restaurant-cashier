package com.restaurant.cashiersystem.util;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityUtils {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前登录用户的ID
     */
    public Long getCurrentUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                return Long.valueOf(claims.getSubject());
            } catch (Exception e) {
                // 忽略
            }
        }
        return null;
    }

    /**
     * 获取当前用户的门店ID，从 JWT token 中解析
     */
    public Long getCurrentStoreId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return null;
        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return claims.get("storeId", Long.class);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }
}