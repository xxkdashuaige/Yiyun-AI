package com.yiyun.controller;

import com.yiyun.common.Result;
import com.yiyun.dto.ChangePasswordRequest;
import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;
import com.yiyun.dto.RegisterRequest;
import com.yiyun.dto.UserInfoResponse;
import com.yiyun.service.AuthService;
import com.yiyun.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口控制器
 * <p>
 * 处理用户登录、注册、用户信息、修改密码等认证相关请求。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(authService.getCurrentUserInfo(userId));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.changePassword(userId, request);
        return Result.success();
    }
}