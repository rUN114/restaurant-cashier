package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.SysConfig;
import com.restaurant.cashiersystem.service.SysConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器
 */
@Tag(name = "系统配置", description = "按Key查询、全量查询、修改配置")
@RestController
@RequestMapping("/api/sys-configs")
public class SysConfigController {

    private static final Logger log = LoggerFactory.getLogger(SysConfigController.class);

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 根据配置key获取配置值
     * @param configKey 配置key
     * @return 配置值
     */
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    @GetMapping("/key/{configKey}")
    public Result<String> getConfigValue(@PathVariable String configKey) {
        String value = sysConfigService.getConfigValue(configKey);
        return Result.success(value);
    }

    /**
     * 查询所有系统配置
     * @return 配置列表
     */
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    @GetMapping
    @Cacheable(value = "configs", key = "'all'")
    public Result<List<SysConfig>> getAllConfigs() {
        List<SysConfig> list = sysConfigService.list();
        return Result.success(list);
    }

    /**
     * 修改系统配置
     * @param config 配置信息
     * @return 无
     */
    @PreAuthorize("hasAuthority('admin_global')")
    @PutMapping
    @CacheEvict(value = "configs", allEntries = true)
    public Result<Void> updateConfig(@RequestBody SysConfig config) {
        sysConfigService.updateById(config);
        return Result.success();
    }
}