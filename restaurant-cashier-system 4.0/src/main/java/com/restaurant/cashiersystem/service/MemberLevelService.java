package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.MemberLevel;

import java.math.BigDecimal;

public interface MemberLevelService extends IService<MemberLevel> {

    /** 根据累计消费查找应升级到的最高等级 */
    MemberLevel findUpgradeLevel(BigDecimal totalConsumption);

    /** 所有启用的等级（按排序升序） */
    java.util.List<MemberLevel> listEnabled();
}
