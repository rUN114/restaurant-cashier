package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {

    /** 根据累计消费金额查找应升级到的最高等级 */
    @Select("SELECT * FROM member_level WHERE min_consumption <= #{totalConsumption} "
            + "AND status = 1 AND deleted_at IS NULL ORDER BY sort_order DESC LIMIT 1")
    MemberLevel findHighestLevelByConsumption(@Param("totalConsumption") BigDecimal totalConsumption);
}
