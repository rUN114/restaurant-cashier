package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Role;
import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户登录验证
     * @param username 账号
     * @param rawPassword 明文密码
     * @return 验证通过返回 User 对象，否则返回 null
     */
    User login(String username, String rawPassword);

    /**
     * 软删除用户（设置 deleted_at = 当前时间）
     */
    void softDeleteUser(Long userId);

    /**
     * 获取用户及其分店信息
     */
    User getUserWithStore(Long userId);

    /**
     * 获取用户及其角色信息
     */
    User getUserWithRole(Long userId);

    /**
     * 获取完整用户信息（分店+角色）
     */
    User getUserWithFullDetails(Long userId);

    /**
     * 获取用户可管理的分店列表（总部管理员可查看所有，分店管理员仅查看本店）
     */
    List<Store> getUserStores(Long userId);

    /**
     * 获取用户的角色
     */
    Role getUserRole(Long userId);

    /**
     * 根据门店ID查询员工列表（店长专用）
     */
    List<User> getUsersByStore(Long storeId);
}