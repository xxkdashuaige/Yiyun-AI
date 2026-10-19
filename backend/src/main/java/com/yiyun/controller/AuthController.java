package com.yiyun.controller;

import com.yiyun.common.Result;
import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;
import com.yiyun.dto.RegisterRequest;
import com.yiyun.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口控制器
 * <p>
 * 处理用户登录、注册等认证相关请求。
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
     *
     * @param request 登录请求（用户名 + 密码）
     * @return Token 和用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户注册
     *
     * @param request 注册请求（用户名 + 密码 + 确认密码 + 昵称）
     * @return 注册成功后直接返回 Token 和用户信息（注册即登录）
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }
}