package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Material;
import com.restaurant.cashiersystem.service.MaterialService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "原料管理", description = "原料 CRUD、按门店查询")
@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    private static final Logger log = LoggerFactory.getLogger(MaterialController.class);

    @Autowired
    private MaterialService materialService;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ 修复：添加admin_global权限
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'chef', 'purchaser')")
    public Result<List<Material>> getMaterialsByStore(@PathVariable Long storeId) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getStoreId, storeId);
        wrapper.orderByAsc(Material::getName);
        return Result.success(materialService.list(wrapper));
    }

    // ✅ 修复：添加admin_global权限
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createMaterial(@RequestBody Material material) {
        materialService.save(material);
        return Result.success(material.getId());
    }

    // ✅ 修复：添加admin_global权限
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateMaterial(@PathVariable Long id, @RequestBody Material material) {
        material.setId(id);
        materialService.updateById(material);
        return Result.success();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store', 'chef', 'purchaser')")
    public Result<List<Material>> getAllMaterials(HttpServletRequest request) {
        // 非全局管理员只看自己门店的原料
        String roleCode = getRoleFromToken(request);
        Long storeId = getStoreIdFromToken(request);
        log.info("getAllMaterials => roleCode={}, storeId={}", roleCode, storeId);
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (!"admin_global".equals(roleCode) && storeId != null) {
            wrapper.eq(Material::getStoreId, storeId);
            log.info("门店过滤模式：查询门店{}的原料", storeId);
        }
        wrapper.orderByAsc(Material::getStoreId).orderByAsc(Material::getName);
        List<Material> result = materialService.list(wrapper);
        log.info("返回原料数量：{}", result.size());
        return Result.success(result);
    }

    private String getRoleFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(header.substring(7));
                return (String) claims.get("roleCode");
            } catch (Exception e) {
                log.error("解析token角色失败", e);
            }
        }
        return null;
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(header.substring(7));
                return claims.get("storeId", Long.class);
            } catch (Exception e) {
                log.error("解析token门店ID失败", e);
            }
        }
        return null;
    }

    // ✅ 修复：添加admin_global权限
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        materialService.removeById(id);
        return Result.success();
    }
}