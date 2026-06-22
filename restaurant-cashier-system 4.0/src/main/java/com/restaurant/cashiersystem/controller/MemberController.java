package com.restaurant.cashiersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.MemberDTO;
import com.restaurant.cashiersystem.dto.MemberPointAdjustDTO;
import com.restaurant.cashiersystem.entity.Member;
import com.restaurant.cashiersystem.entity.MemberLevel;
import com.restaurant.cashiersystem.entity.MemberPointLog;
import com.restaurant.cashiersystem.service.MemberLevelService;
import com.restaurant.cashiersystem.service.MemberPointLogService;
import com.restaurant.cashiersystem.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "会员管理", description = "会员CRUD、积分调整、查询")
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    @Autowired private MemberService memberService;
    @Autowired private MemberLevelService memberLevelService;
    @Autowired private MemberPointLogService pointLogService;

    // ==================== 会员CRUD ====================

    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<List<Member>> list() {
        List<Member> list = memberService.list();
        fillLevels(list);
        return Result.success(list);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public PageResult<List<Member>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Member::getPhone, keyword)
                    .or().like(Member::getName, keyword));
        }
        wrapper.orderByDesc(Member::getCreatedAt);
        Page<Member> page = new Page<>(pageNum, pageSize);
        memberService.page(page, wrapper);
        fillLevels(page.getRecords());
        return PageResult.success(page.getRecords(), page.getTotal(),
                page.getPages(), (int) page.getCurrent(), (int) page.getSize());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<Member> create(@RequestBody MemberDTO dto) {
        Member member = new Member();
        BeanUtils.copyProperties(dto, member);
        member.setStatus(1);
        MemberLevel defaultLevel = memberLevelService.findUpgradeLevel(
                member.getTotalConsumption() != null ? member.getTotalConsumption() : java.math.BigDecimal.ZERO);
        if (defaultLevel != null) member.setLevelId(defaultLevel.getId());
        memberService.save(member);
        return Result.success(member);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global','admin_store')")
    public Result<Void> update(@PathVariable Long id, @RequestBody MemberDTO dto) {
        Member member = memberService.getById(id);
        if (member == null) return Result.error("会员不存在");
        BeanUtils.copyProperties(dto, member, "id", "phone", "status", "levelId",
                "totalPoints", "availablePoints", "totalConsumption", "totalOrders");
        member.setId(id);
        memberService.updateById(member);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('admin_global')")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status != 0 && status != 1) return Result.error("状态值无效，仅支持 0(禁用) 或 1(启用)");
        Member member = memberService.getById(id);
        if (member == null) return Result.error("会员不存在");
        member.setStatus(status);
        memberService.updateById(member);
        log.info("会员状态变更: id={}, status={}", id, status);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Member> getById(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member != null && member.getLevelId() != null) {
            member.setLevel(memberLevelService.getById(member.getLevelId()));
        }
        return Result.success(member);
    }

    @GetMapping("/search")
    @Operation(summary = "收银台-手机号搜索会员")
    public Result<Member> searchByPhone(@RequestParam String phone) {
        Member member = memberService.findByPhone(phone);
        // 统一返回消息，防止通过不同错误信息枚举手机号
        if (member == null || member.getStatus() != 1) return Result.error("未找到该手机号的会员");
        if (member.getLevelId() != null) {
            member.setLevel(memberLevelService.getById(member.getLevelId()));
        }
        return Result.success(member);
    }

    @PostMapping("/register")
    @Operation(summary = "收银台-手机号快速注册会员")
    public Result<Member> register(@RequestBody MemberDTO dto) {
        try {
            Member member = memberService.registerOrFind(dto.getPhone(), dto.getName(), dto.getStoreId());
            if (member.getLevelId() != null) {
                member.setLevel(memberLevelService.getById(member.getLevelId()));
            }
            return Result.success(member);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 积分调整 ====================

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/points/adjust")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> adjustPoints(@RequestBody MemberPointAdjustDTO dto) {
        Member member = memberService.findByIdForUpdate(dto.getMemberId());
        if (member == null) return Result.error("会员不存在");

        int before = member.getAvailablePoints();
        int change = dto.getPoints();
        int after = before + change;
        if (after < 0) return Result.error("积分不足，扣减后不能为负");

        // 直接更新积分（FOR UPDATE 锁已持有），避免调用 addPoints/deductPoints
        // 造成双重 pointLogService.log() 记账
        Member update = new Member();
        update.setId(dto.getMemberId());
        update.setAvailablePoints(after);
        if (change > 0) {
            update.setTotalPoints(member.getTotalPoints() + change);
        }
        memberService.updateById(update);

        int logType = change > 0 ? 4 : 5;
        pointLogService.log(dto.getMemberId(), null, logType,
                change, before, after, null, dto.getRemark(), null);

        log.info("手动调整积分: memberId={}, change={}, remark={}", dto.getMemberId(), change, dto.getRemark());
        return Result.success();
    }

    // ==================== 积分流水 ====================

    @GetMapping("/{id}/point-logs")
    public PageResult<List<MemberPointLog>> pointLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<MemberPointLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberPointLog::getMemberId, id)
                .orderByDesc(MemberPointLog::getCreatedAt);
        Page<MemberPointLog> page = new Page<>(pageNum, pageSize);
        pointLogService.page(page, wrapper);
        return PageResult.success(page.getRecords(), page.getTotal(),
                page.getPages(), (int) page.getCurrent(), (int) page.getSize());
    }

    /** 批量填充会员等级：一次 listByIds 替代逐条 getById，消除 N+1 */
    private void fillLevels(List<Member> members) {
        Set<Long> levelIds = members.stream()
                .map(Member::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (levelIds.isEmpty()) return;

        Map<Long, MemberLevel> levelMap = memberLevelService.listByIds(levelIds).stream()
                .collect(Collectors.toMap(MemberLevel::getId, l -> l));
        for (Member m : members) {
            if (m.getLevelId() != null) {
                m.setLevel(levelMap.get(m.getLevelId()));
            }
        }
    }
}
