package com.yiyun.service;

import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;

/**
 * 认证服务接口
 * <p>
 * 定义登录、登出等认证相关的业务方法。
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求（用户名 + 密码）
     * @return 登录响应（Token + 用户信息）
     */
    LoginResponse login(LoginRequest request);
}