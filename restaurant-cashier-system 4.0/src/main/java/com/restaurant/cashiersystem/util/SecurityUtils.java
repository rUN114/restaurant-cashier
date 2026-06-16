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
     * 获取当前用户的门店ID（可选，从 token 中解析，需确保 token 包含 storeId）
     */
    public Long getCurrentStoreId() {
        // 如果 token 中存了 storeId，可以类似解析
        return null; // 若需要可自己扩展
    }
}