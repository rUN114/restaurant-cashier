package com.restaurant.cashiersystem.filter;

import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.service.StoreService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 不受分店停用限制的路径前缀 */
    private static final List<String> STORE_CHECK_EXCLUDE_PREFIXES = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/captcha",
            "/api/changelogs",
            "/api/stores",
            "/api/roles",
            "/api/sys-configs",
            "/swagger-ui",
            "/v3/api-docs",
            "/images",
            "/favicon.ico"
    );

    /** token 黑名单 Redis key 前缀 */
    private static final String TOKEN_REVOKED_PREFIX = "token:revoked:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 记录请求 IP 到线程上下文，供操作日志使用
        com.restaurant.cashiersystem.util.RequestContext.setClientIp(getClientIp(request));
        try {
            doFilterLogic(request, response, filterChain);
        } finally {
            com.restaurant.cashiersystem.util.RequestContext.clear();
        }
    }

    private void doFilterLogic(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                String userId = claims.getSubject();
                String roleCode = (String) claims.get("roleCode");

                if (roleCode != null) {
                    // ========== JWT 黑名单校验：用户被禁用/删除后，已签发的 token 即时失效 ==========
                    Date issuedAt = claims.getIssuedAt();
                    if (issuedAt != null && isTokenRevoked(userId, issuedAt)) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write(
                            "{\"code\":401,\"msg\":\"账号已被禁用，token 已失效，请重新登录\",\"data\":null}"
                        );
                        return;
                    }
                    // ========== 黑名单校验结束 ==========

                    // ========== 分店停用/删除校验（非 admin_global 用户） ==========
                    if (!"admin_global".equals(roleCode)) {
                        Long storeId = claims.get("storeId", Long.class);
                        if (storeId != null && !isExcludedPath(request.getRequestURI())) {
                            Store store = storeService.getStoreById(storeId);
                            if (store == null) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write(
                                    "{\"code\":403,\"msg\":\"所属分店已删除，操作被拒绝\",\"data\":null}"
                                );
                                return;
                            }
                            if (store.getStatus() != null && store.getStatus() == 0) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write(
                                    "{\"code\":403,\"msg\":\"所属分店已停用，操作被拒绝\",\"data\":null}"
                                );
                                return;
                            }
                        }
                    }
                    // ========== 分店状态校验结束 ==========

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(roleCode)
                    );
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (ExpiredJwtException e) {
                log.debug("JWT 已过期: {}", e.getMessage());
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                    "{\"code\":401,\"msg\":\"登录已过期，请重新登录\",\"data\":null}"
                );
                return;
            } catch (Exception e) {
                log.debug("JWT 解析失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }

    /**
     * 判断 token 是否已被撤销：token 签发时间早于黑名单时间戳则视为失效
     */
    private boolean isTokenRevoked(String userId, Date issuedAt) {
        try {
            String revoked = stringRedisTemplate.opsForValue()
                    .get(TOKEN_REVOKED_PREFIX + userId);
            if (revoked != null) {
                long revokedAt = Long.parseLong(revoked);
                return issuedAt.getTime() <= revokedAt;
            }
        } catch (Exception e) {
            log.warn("JWT 黑名单查询失败, userId={}: {}", userId, e.getMessage());
        }
        return false;
    }

    /** 判断请求路径是否属于免检列表 */
    private boolean isExcludedPath(String uri) {
        for (String prefix : STORE_CHECK_EXCLUDE_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }
}
