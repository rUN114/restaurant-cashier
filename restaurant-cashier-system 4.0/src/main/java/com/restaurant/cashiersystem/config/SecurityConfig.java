package com.restaurant.cashiersystem.config;

import com.restaurant.cashiersystem.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * CORS 允许的来源域名列表，通过环境变量 CORS_ORIGINS 配置，逗号分隔。
     * 默认仅允许本地开发环境。生产环境必须显式配置。
     * 示例: export CORS_ORIGINS=https://pos.example.com,https://admin.example.com
     */
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:5173,http://localhost:8080,http://127.0.0.1:5173,http://127.0.0.1:8080}")
    private String allowedOriginsConfig;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = Arrays.asList(allowedOriginsConfig.split(","));
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 启用 CORS
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // 允许被 iframe 嵌入
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 放行 OPTIONS 预检请求（重要！）
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        // 放行登录、验证码、会员查询等公开接口
                        .requestMatchers("/api/users/login", "/api/users/register",
                                "/api/captcha/**", "/api/members/search", "/api/members/register",
                                "/images/**", "/favicon.ico").permitAll()
                        // 会员结算需要认证
                        .requestMatchers("/api/member-checkout/**").authenticated()
                        // 更新日志（列表和最新版本所有用户可查看，新增/编辑/删除仅管理员）
                        .requestMatchers(HttpMethod.GET, "/api/changelogs", "/api/changelogs/latest").permitAll()
                        .requestMatchers("/api/changelogs/**").hasAnyAuthority("admin_global", "admin_store")
                        // 放行 Swagger UI 和 OpenAPI 文档
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 放行 Actuator 健康检查（容器探活用）
                        .requestMatchers("/actuator/health/**").permitAll()
                        // 放行 WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}