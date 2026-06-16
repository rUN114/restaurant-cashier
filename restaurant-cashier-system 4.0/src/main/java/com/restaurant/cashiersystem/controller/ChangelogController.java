package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Changelog;
import com.restaurant.cashiersystem.service.ChangelogService;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "更新日志", description = "版本更新日志管理")
@RestController
@RequestMapping("/api/changelogs")
public class ChangelogController {
    private static final Logger log = LoggerFactory.getLogger(ChangelogController.class);

    @Autowired
    private ChangelogService changelogService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 列表（所有登录用户可查看）
     */
    @GetMapping
    public Result<List<Changelog>> list() {
        LambdaQueryWrapper<Changelog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Changelog::getCreatedAt);
        return Result.success(changelogService.list(wrapper));
    }

    /**
     * 获取最新一条（首页版本号用）
     */
    @GetMapping("/latest")
    public Result<Changelog> latest() {
        LambdaQueryWrapper<Changelog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Changelog::getCreatedAt);
        wrapper.last("LIMIT 1");
        Changelog latest = changelogService.getOne(wrapper);
        return Result.success(latest);
    }

    /**
     * 新增（仅管理员）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    @Operation(summary = "上传更新日志")
    public Result<Long> create(@RequestBody Changelog changelog, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        changelog.setCreatedBy(userId);
        changelogService.save(changelog);
        log.info("新增更新日志 v{}: {}", changelog.getVersion(), changelog.getTitle());
        return Result.success(changelog.getId());
    }

    /**
     * 编辑（仅管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    @Operation(summary = "编辑更新日志")
    public Result<Void> update(@PathVariable Long id, @RequestBody Changelog changelog) {
        changelog.setId(id);
        changelogService.updateById(changelog);
        log.info("编辑更新日志 id={}, v{}", id, changelog.getVersion());
        return Result.success();
    }

    /**
     * 删除（仅管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<Void> delete(@PathVariable Long id) {
        changelogService.removeById(id);
        return Result.success();
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return Long.valueOf(claims.getSubject());
            } catch (Exception e) {
                log.error("解析token失败", e);
            }
        }
        return null;
    }
}
