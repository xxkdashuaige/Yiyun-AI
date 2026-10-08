package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;
import com.yiyun.entity.SysUser;
import com.yiyun.mapper.SysUserMapper;
import com.yiyun.service.AuthService;
import com.yiyun.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 * <p>
 * 校验用户名密码，生成 JWT Token。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 登录逻辑：
     * 1. 根据用户名查询用户
     * 2. 校验用户状态和密码
     * 3. 生成 JWT Token 返回
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 2. 校验状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        // 3. 校验密码（BCrypt 比对）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 Token 并返回用户信息
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());
    }
}