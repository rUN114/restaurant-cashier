package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.MemberLevel;
import com.restaurant.cashiersystem.mapper.MemberLevelMapper;
import com.restaurant.cashiersystem.service.MemberLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel>
        implements MemberLevelService {

    @Autowired
    private MemberLevelMapper memberLevelMapper;

    @Override
    public MemberLevel findUpgradeLevel(BigDecimal totalConsumption) {
        return memberLevelMapper.findHighestLevelByConsumption(totalConsumption);
    }

    @Override
    public List<MemberLevel> listEnabled() {
        return list(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getSortOrder));
    }
}
