package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.service.AIService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI建议", description = "DeepSeek 驱动的明日预制建议")
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired private AIService aiService;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store','chef')")
    public Result<List<Map<String, Object>>> suggest(HttpServletRequest request) {
        Long storeId = getStoreId(request);
        return Result.success(aiService.suggest(storeId));
    }

    private Long getStoreId(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(h.substring(7));
                String role = (String) claims.get("roleCode");
                if ("admin_global".equals(role)) return null;
                return claims.get("storeId", Long.class);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
