package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /** 根据手机号查找 */
    @Select("SELECT * FROM member WHERE phone = #{phone} AND deleted_at IS NULL")
    Member selectByPhone(@Param("phone") String phone);

    /** 加锁查询（防并发） */
    @Select("SELECT * FROM member WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    Member selectByIdForUpdate(@Param("id") Long id);

    /** 增加可用积分 */
    @Update("UPDATE member SET available_points = available_points + #{points}, "
            + "total_points = total_points + #{points} WHERE id = #{id}")
    int addPoints(@Param("id") Long id, @Param("points") int points);

    /** 扣减可用积分 */
    @Update("UPDATE member SET available_points = available_points - #{points} WHERE id = #{id} "
            + "AND available_points >= #{points}")
    int deductPoints(@Param("id") Long id, @Param("points") int points);

    /** 更新累计消费 */
    @Update("UPDATE member SET total_consumption = total_consumption + #{amount}, "
            + "total_orders = total_orders + 1, last_consumption_time = NOW() "
            + "WHERE id = #{id}")
    int addConsumption(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 更新会员等级 */
    @Update("UPDATE member SET level_id = #{levelId} WHERE id = #{id}")
    int updateLevel(@Param("id") Long id, @Param("levelId") Long levelId);
}
