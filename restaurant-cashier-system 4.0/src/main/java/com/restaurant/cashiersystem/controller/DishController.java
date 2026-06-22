package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.entity.Dish;
import com.restaurant.cashiersystem.entity.OperationLog;
import com.restaurant.cashiersystem.service.DishService;
import com.restaurant.cashiersystem.service.OperationLogService;
import com.restaurant.cashiersystem.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 菜品管理控制器
 */
@Tag(name = "菜品管理", description = "菜品 CRUD、图片上传、按门店/可售状态查询")
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private static final Logger log = LoggerFactory.getLogger(DishController.class);

    @Autowired
    private DishService dishService;

    @Autowired
    private OperationLogService operationLogService;   // 新增：操作日志服务

    @Autowired
    private SecurityUtils securityUtils;               // 新增：安全工具类

    @Value("${file.upload.path:./upload-images/}")
    private String uploadPath;

    /**
     * 根据门店ID查询菜品列表
     */
    @GetMapping("/store/{storeId}")
    public Result<List<Dish>> getDishesByStore(@PathVariable Long storeId) {
        List<Dish> list = dishService.getDishesByStore(storeId);
        return Result.success(list);
    }

    /**
     * 根据门店ID查询【可售状态】的菜品
     */
    @GetMapping("/available/{storeId}")
    public Result<List<Dish>> getAvailableDishes(@PathVariable Long storeId) {
        List<Dish> list = dishService.getAvailableDishes(storeId);
        return Result.success(list);
    }

    /**
     * 新增菜品（记录操作日志）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createDish(@RequestBody Dish dish) {
        dishService.save(dish);
        // 记录操作日志
        try {
            OperationLog opLog = new OperationLog();
            opLog.setStoreId(dish.getStoreId());
            opLog.setOperatorId(securityUtils.getCurrentUserId());
            opLog.setTargetType("dish");
            opLog.setTargetId(dish.getId().toString());
            opLog.setAction("createDish");
            opLog.setDetail("新增菜品：" + dish.getName() + "，价格：" + dish.getPrice());
            opLog.setCreatedAt(LocalDateTime.now());
            operationLogService.save(opLog);
        } catch (Exception e) {
            log.error("记录菜品新增日志失败", e);
        }
        return Result.success(dish.getId());
    }

    /**
     * 修改菜品信息（记录操作日志）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateDish(@PathVariable Long id, @RequestBody Dish dish) {
        dish.setId(id);
        dishService.updateById(dish);
        // 记录操作日志
        try {
            Dish updatedDish = dishService.getById(id);
            OperationLog opLog = new OperationLog();
            opLog.setStoreId(updatedDish != null ? updatedDish.getStoreId() : null);
            opLog.setOperatorId(securityUtils.getCurrentUserId());
            opLog.setTargetType("dish");
            opLog.setTargetId(id.toString());
            opLog.setAction("updateDish");
            opLog.setDetail("修改菜品：" + dish.getName() + "，价格：" + dish.getPrice() + "，状态：" + (dish.getIsAvailable() == 1 ? "上架" : "下架"));
            opLog.setCreatedAt(LocalDateTime.now());
            operationLogService.save(opLog);
        } catch (Exception e) {
            log.error("记录菜品修改日志失败", e);
        }
        return Result.success();
    }

    /**
     * 逻辑删除菜品（记录操作日志）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteDish(@PathVariable Long id) {
        Dish dish = dishService.getById(id);
        if (dish != null) {
            // 记录操作日志
            try {
                OperationLog opLog = new OperationLog();
                opLog.setStoreId(dish.getStoreId());
                opLog.setOperatorId(securityUtils.getCurrentUserId());
                opLog.setTargetType("dish");
                opLog.setTargetId(id.toString());
                opLog.setAction("deleteDish");
                opLog.setDetail("删除菜品：" + dish.getName());
                opLog.setCreatedAt(LocalDateTime.now());
                operationLogService.save(opLog);
            } catch (Exception e) {
                log.error("记录菜品删除日志失败", e);
            }
        }
        dishService.deleteDishLogic(id);
        return Result.success();
    }

    /**
     * 上传菜品图片
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件为空");
        }
        // 文件类型白名单
        Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            return Result.error("仅支持 JPG/PNG/GIF/WebP 格式图片");
        }
        // 文件大小限制 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("图片大小不能超过 5MB");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                suffix = ".jpg";
            }
            String newFileName = UUID.randomUUID().toString() + suffix;

            String basePath = uploadPath;
            if (!basePath.endsWith("/") && !basePath.endsWith("\\")) {
                basePath += File.separator;
            }
            File dir = new File(basePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(basePath + newFileName);
            file.transferTo(dest);

            String imageUrl = "/images/" + newFileName;
            log.info("菜品图片上传成功：{}", imageUrl);
            return Result.success(imageUrl);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 查询所有菜品（仅管理员）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin_global')")
    public Result<List<Dish>> getAllDishes() {
        List<Dish> list = dishService.list();
        return Result.success(list);
    }
}