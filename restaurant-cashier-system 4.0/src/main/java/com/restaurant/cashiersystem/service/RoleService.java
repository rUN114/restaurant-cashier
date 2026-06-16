package com.restaurant.cashiersystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.cashiersystem.entity.Role;

public interface RoleService extends IService<Role> {
    Role getRoleById(Long roleId);
    Role getRoleByCode(String roleCode);
}