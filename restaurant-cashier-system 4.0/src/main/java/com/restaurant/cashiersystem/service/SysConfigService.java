package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.SysConfig;

/**
 * 系统配置接口
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据key获取配置值
     */
    String getConfigValue(String configKey);

    /** 获取整数配置 */
    int getIntValue(String configKey, int defaultValue);

    /** 获取布尔配置 */
    boolean getBoolValue(String configKey, boolean defaultValue);
}