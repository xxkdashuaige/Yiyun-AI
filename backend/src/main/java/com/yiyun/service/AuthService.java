package com.yiyun.service;

import com.yiyun.dto.ChangePasswordRequest;
import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;
import com.yiyun.dto.RegisterRequest;
import com.yiyun.dto.UserInfoResponse;

/**
 * 认证服务接口
 * <p>
 * 定义登录、注册、用户信息、修改密码等认证相关的业务方法。
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求（用户名 + 密码）
     * @return 登录响应（Token + 用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求（用户名 + 密码 + 确认密码 + 昵称）
     * @return 注册成功后直接返回登录响应（Token + 用户信息）
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserInfoResponse getCurrentUserInfo(Long userId);

    /**
     * 修改密码
     *
     * @param userId  用户 ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}