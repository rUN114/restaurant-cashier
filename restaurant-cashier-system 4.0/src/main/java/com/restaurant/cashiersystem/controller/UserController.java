package com.restaurant.cashiersystem.controller;

import com.restaurant.cashiersystem.common.PageResult;
import com.restaurant.cashiersystem.common.Result;
import com.restaurant.cashiersystem.dto.LoginRequest;
import com.restaurant.cashiersystem.dto.LoginResponse;
import com.restaurant.cashiersystem.dto.UserDTO;
import com.restaurant.cashiersystem.entity.LoginLog;
import com.restaurant.cashiersystem.entity.Role;
import com.restaurant.cashiersystem.entity.Store;
import com.restaurant.cashiersystem.entity.User;
import com.restaurant.cashiersystem.service.LoginLogService;
import com.restaurant.cashiersystem.service.UserService;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.restaurant.cashiersystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "用户管理", description = "用户登录、注册、CRUD 及关联查询")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LoginLogService loginLogService;   // 新增

    @Autowired
    private ImageCaptchaApplication captchaApplication;

    @Value("${captcha.tianai.enabled:true}")
    private boolean captchaEnabled;

    // ===================== 基础 CRUD =====================
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<List<User>> getAllUsers(HttpServletRequest request) {
        // 店长只看本店员工，管理员看全部
        String roleCode = getRoleFromToken(request);
        Long userStoreId = getStoreIdFromToken(request);
        log.info("getAllUsers => roleCode={}, storeId={}", roleCode, userStoreId);
        List<User> users;
        if ("admin_store".equals(roleCode) && userStoreId != null) {
            users = userService.getUsersByStore(userStoreId);
            log.info("店长模式：查询门店{}的员工，共{}人", userStoreId, users.size());
        } else {
            users = userService.list();
            log.info("管理员模式：查询全部员工，共{}人", users.size());
        }
        return Result.success(users);
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public PageResult<List<User>> getUsersPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        String roleCode = getRoleFromToken(request);
        Long userStoreId = getStoreIdFromToken(request);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if ("admin_store".equals(roleCode) && userStoreId != null) {
            wrapper.eq(User::getStoreId, userStoreId);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        userService.page(page, wrapper);

        return PageResult.success(page.getRecords(), page.getTotal(), page.getPages(),
            (int) page.getCurrent(), (int) page.getSize());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Long> createUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        // 店长只能创建本店员工，强制覆盖 storeId
        String roleCode = getRoleFromToken(request);
        if ("admin_store".equals(roleCode)) {
            user.setStoreId(getStoreIdFromToken(request));
        }
        userService.save(user);
        return Result.success(user.getId());
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setId(id);
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyAuthority('admin_global', 'admin_store')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id:\\d+}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    @GetMapping("/{id:\\d+}/with-store")
    public Result<User> getUserWithStore(@PathVariable Long id) {
        User user = userService.getUserWithStore(id);
        return Result.success(user);
    }

    @GetMapping("/{id:\\d+}/with-role")
    public Result<User> getUserWithRole(@PathVariable Long id) {
        User user = userService.getUserWithRole(id);
        return Result.success(user);
    }

    @GetMapping("/{id:\\d+}/full")
    public Result<User> getUserFullDetails(@PathVariable Long id) {
        User user = userService.getUserWithFullDetails(id);
        return Result.success(user);
    }

    @GetMapping("/{id:\\d+}/stores")
    public Result<List<Store>> getUserStores(@PathVariable Long id) {
        List<Store> stores = userService.getUserStores(id);
        return Result.success(stores);
    }

    @GetMapping("/{id:\\d+}/roles")
    public Result<Role> getUserRole(@PathVariable Long id) {
        Role role = userService.getUserRole(id);
        return Result.success(role);
    }

    // ===================== 登录 =====================
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest,
                                       HttpServletRequest request) {

        // ---- 滑块验证码校验（dev 环境自动跳过） ----
        if (captchaEnabled) {
            String captchaId = loginRequest.getCaptchaId();
            if (captchaId == null || captchaId.isBlank()) {
                return Result.error("验证码参数缺失，请完成滑块验证");
            }

            ImageCaptchaTrack track = buildCaptchaTrack(loginRequest);
            cloud.tianai.captcha.common.response.ApiResponse<?> matchResult =
                    captchaApplication.matching(captchaId, track);
            if (!matchResult.isSuccess()) {
                log.warn("CAPTCHA 验证失败: id={}, msg={}", captchaId, matchResult.getMsg());
                return Result.error("验证码验证失败，请重试");
            }
            log.info("CAPTCHA 验证通过: id={}", captchaId);
        }
        // ---- 验证码校验结束 ----

        LoginLog loginLog = new LoginLog();
        loginLog.setLoginIp(getClientIp(request));
        loginLog.setLoginDevice(request.getHeader("User-Agent"));
        loginLog.setCreatedAt(LocalDateTime.now());

        try {
            log.info("用户登录请求：username={}", loginRequest.getUsername());
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

            if (user == null) {
                loginLog.setLoginStatus(0);
                loginLog.setStoreId(null);
                loginLogService.save(loginLog);
                log.error("登录失败：用户名或密码错误");
                return Result.error("用户名或密码错误");
            }

            if (user.getStatus() == null || user.getStatus() != 1 || user.getDeletedAt() != null) {
                loginLog.setLoginStatus(0);
                loginLog.setUserId(user.getId());
                loginLog.setStoreId(user.getStoreId());
                loginLogService.save(loginLog);
                log.error("登录失败：账号已禁用或已删除");
                return Result.error("账号已禁用或已删除");
            }

            // 登录成功
            loginLog.setLoginStatus(1);
            loginLog.setUserId(user.getId());
            loginLog.setStoreId(user.getStoreId());
            loginLogService.save(loginLog);

            Role role = userService.getUserRole(user.getId());
            String roleCode = role != null ? role.getRoleCode() : null;

            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode, user.getStoreId());

            LoginResponse response = new LoginResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getRealName(),
                    user.getStoreId(),
                    roleCode
            );
            log.info("登录成功：userId={}, username={}", user.getId(), user.getUsername());
            return Result.success(response);
        } catch (Exception e) {
            loginLog.setLoginStatus(0);
            loginLogService.save(loginLog);
            log.error("登录异常：", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/current")
    public Result<LoginResponse> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("未提供Token或格式错误");
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = (String) claims.get("username");

            User user = userService.getById(userId);
            if (user == null || user.getDeletedAt() != null || user.getStatus() != 1) {
                return Result.error("用户不存在或已禁用");
            }
            Role role = userService.getUserRole(userId);
            String roleCode = role != null ? role.getRoleCode() : null;

            LoginResponse response = new LoginResponse(
                    token,
                    userId,
                    username,
                    user.getRealName(),
                    user.getStoreId(),
                    roleCode
            );
            return Result.success(response);
        } catch (ExpiredJwtException e) {
            return Result.error("Token已过期");
        } catch (JwtException | IllegalArgumentException e) {
            return Result.error("无效的Token");
        }
    }

    // ===================== 工具方法 =====================
    private String getRoleFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return (String) claims.get("roleCode");
            } catch (Exception e) {
                log.error("解析token中的角色失败", e);
            }
        }
        return null;
    }

    private Long getStoreIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(authHeader.substring(7));
                return claims.get("storeId", Long.class);
            } catch (Exception e) {
                log.error("解析token中的门店ID失败", e);
            }
        }
        return null;
    }

    /**
     * 将前端传入的轨迹数据转换为 tianai-captcha 的 ImageCaptchaTrack
     */
    private ImageCaptchaTrack buildCaptchaTrack(LoginRequest loginRequest) {
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setBgImageWidth(loginRequest.getBgImageWidth());
        track.setBgImageHeight(loginRequest.getBgImageHeight());
        track.setTemplateImageWidth(loginRequest.getSliderImageWidth());
        track.setTemplateImageHeight(loginRequest.getSliderImageHeight());
        track.setStartTime(loginRequest.getStartTime());
        track.setStopTime(loginRequest.getStopTime());

        List<ImageCaptchaTrack.Track> trackList = new ArrayList<>();
        if (loginRequest.getTrackList() != null) {
            for (LoginRequest.TrackPoint pt : loginRequest.getTrackList()) {
                ImageCaptchaTrack.Track t = new ImageCaptchaTrack.Track();
                t.setX(pt.getX() != null ? pt.getX().floatValue() : 0f);
                t.setY(pt.getY() != null ? pt.getY().floatValue() : 0f);
                t.setT(pt.getT() != null ? pt.getT().floatValue() : 0f);
                trackList.add(t);
            }
        }
        track.setTrackList(trackList);
        return track;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}