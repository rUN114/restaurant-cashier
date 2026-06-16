package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Role;
import com.restaurant.cashiersystem.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色权限控制器
 */
@Tag(name = "角色管理", description = "角色 CRUD、按编码查询")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    /**
     * 查询所有角色
     * @return 角色列表
     */
    @GetMapping
    @Cacheable(value = "roles", key = "'all'")
    public Result<List<Role>> getAllRoles() {
        List<Role> list = roleService.list();
        return Result.success(list);
    }

    /**
     * 根据ID查询角色详情
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public Result<Role> getRoleById(@PathVariable Long id) {
        Role role = roleService.getById(id);
        return Result.success(role);
    }

    /**
     * 根据角色编码查询角色
     * @param roleCode 角色编码
     * @return 角色信息
     */
    @GetMapping("/code/{roleCode}")
    public Result<Role> getRoleByCode(@PathVariable String roleCode) {
        Role role = roleService.getRoleByCode(roleCode);
        return Result.success(role);
    }

    /**
     * 新增角色
     * 权限：只有管理员
     * @param role 角色信息
     * @return 角色ID
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin_global')")
    @CacheEvict(value = "roles", allEntries = true)
    public Result<Long> createRole(@RequestBody Role role) {
        roleService.save(role);
        return Result.success(role.getId());
    }

    /**
     * 修改角色信息
     * 权限：只有管理员
     * @param id 角色ID
     * @param role 角色信息
     * @return 无
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin_global')")
    @CacheEvict(value = "roles", allEntries = true)
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        // 只更新 name 和 roleCode（如果允许）或仅 name
        // 为避免意外覆盖，可仅设置 name
        Role updateRole = new Role();
        updateRole.setId(id);
        updateRole.setName(role.getName());   // 明确更新名称
        updateRole.setDescription(role.getDescription()); // 如需
        roleService.updateById(updateRole);
        return Result.success();
    }



    /**
     * 删除角色
     * 权限：只有管理员
     * @param id 角色ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }
}