package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.SysConfig;
import com.restaurant.cashiersystem.mapper.SysConfigMapper;
import com.restaurant.cashiersystem.service.SysConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置实现
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig config = getOne(wrapper);
        return config == null ? null : config.getConfigValue();
    }

    @Override
    public int getIntValue(String configKey, int defaultValue) {
        String val = getConfigValue(configKey);
        if (val == null || val.isBlank()) return defaultValue;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    @Override
    public boolean getBoolValue(String configKey, boolean defaultValue) {
        String val = getConfigValue(configKey);
        if (val == null || val.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim());
    }
}