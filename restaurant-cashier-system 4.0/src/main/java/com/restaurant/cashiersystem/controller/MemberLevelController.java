package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.MemberLevelDTO;
import com.restaurant.cashiersystem.entity.MemberLevel;
import com.restaurant.cashiersystem.service.MemberLevelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "会员等级配置", description = "会员等级CRUD、折扣配置")
@RestController
@RequestMapping("/api/member-levels")
public class MemberLevelController {

    @Autowired private MemberLevelService memberLevelService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<MemberLevel>> list() {
        return Result.success(memberLevelService.listEnabled());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global')")
    public Result<MemberLevel> create(@RequestBody MemberLevelDTO dto) {
        MemberLevel level = new MemberLevel();
        BeanUtils.copyProperties(dto, level);
        level.setStatus(1);
        memberLevelService.save(level);
        return Result.success(level);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberLevelDTO dto) {
        MemberLevel level = memberLevelService.getById(id);
        if (level == null) return Result.error("等级不存在");
        BeanUtils.copyProperties(dto, level, "id");
        level.setId(id);
        memberLevelService.updateById(level);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global')")
    public Result<Void> delete(@PathVariable Long id) {
        memberLevelService.removeById(id);
        return Result.success();
    }
}
