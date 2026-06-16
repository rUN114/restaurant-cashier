package com.restaurant.cashiersystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.cashiersystem.entity.Role;
import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.entity.User;
import com.restaurant.cashiersystem.mapper.RoleMapper;
import com.restaurant.cashiersystem.mapper.StoreMapper;
import com.restaurant.cashiersystem.mapper.UserMapper;
import com.restaurant.cashiersystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private StoreMapper storeMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User login(String username, String rawPassword) {
        // 查询用户（已过滤 deleted_at is null）
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        // 验证密码
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setDeletedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public User getUserWithStore(Long userId) {
        return userMapper.selectUserWithStoreById(userId);
    }

    @Override
    public User getUserWithRole(Long userId) {
        return userMapper.selectUserWithRoleById(userId);
    }

    @Override
    public User getUserWithFullDetails(Long userId) {
        User user = getUserWithStore(userId);
        if (user != null) {
            Role role = getUserRole(userId);
            user.setRole(role);  // 需要 User 中有 role 字段（@TableField(exist=false)）
        }
        return user;
    }

    @Override
    public List<Store> getUserStores(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        // 判断是否是总部管理员（通过 role_code 或 store_id 为 null）
        Role role = getUserRole(userId);
        if (role != null && "admin_global".equals(role.getRoleCode())) {
            // 总部管理员可查看所有未删除的分店
            LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(Store::getDeletedAt);
            return storeMapper.selectList(wrapper);
        } else {
            // 分店管理员或普通员工只能看到自己所在分店
            Store store = storeMapper.selectById(user.getStoreId());
            List<Store> list = new ArrayList<>();
            if (store != null && store.getDeletedAt() == null) {
                list.add(store);
            }
            return list;
        }
    }

    @Override
    public Role getUserRole(Long userId) {
        User user = getById(userId);
        if (user == null || user.getRoleId() == null) {
            return null;
        }
        return roleMapper.selectById(user.getRoleId());
    }

    @Override
    public List<User> getUsersByStore(Long storeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStoreId, storeId);
        return list(wrapper);
    }
}