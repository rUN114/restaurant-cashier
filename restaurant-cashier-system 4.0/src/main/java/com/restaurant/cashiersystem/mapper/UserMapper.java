package com.restaurant.cashiersystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.cashiersystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（含逻辑删除过滤）
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted_at IS NULL")
    User selectByUsername(@Param("username") String username);

    /**
     * 查询用户及其分店信息（一对一）
     */
    @Select("SELECT u.*, s.name as store_name, s.address as store_address, s.phone as store_phone " +
            "FROM users u LEFT JOIN stores s ON u.store_id = s.id " +
            "WHERE u.id = #{id} AND u.deleted_at IS NULL")
    User selectUserWithStoreById(@Param("id") Long id);

    /**
     * 查询用户及其角色信息（多对一）
     */
    @Select("SELECT u.*, r.role_code, r.name as role_name " +
            "FROM users u LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE u.id = #{id} AND u.deleted_at IS NULL")
    User selectUserWithRoleById(@Param("id") Long id);
}