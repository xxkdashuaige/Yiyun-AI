package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiyun.dto.ChangePasswordRequest;
import com.yiyun.dto.LoginRequest;
import com.yiyun.dto.LoginResponse;
import com.yiyun.dto.RegisterRequest;
import com.yiyun.dto.UserInfoResponse;
import com.yiyun.entity.SysUser;
import com.yiyun.mapper.SysUserMapper;
import com.yiyun.service.AuthService;
import com.yiyun.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * 注册逻辑：
     * 1. 校验两次密码一致
     * 2. 校验用户名唯一性
     * 3. BCrypt 加密密码
     * 4. 保存用户（默认状态正常）
     * 5. 直接生成 Token 返回（注册即登录）
     */
    @Override
    public LoginResponse register(RegisterRequest request) {
        // 1. 校验两次密码一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 2. 校验用户名唯一性
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new RuntimeException("用户名已被注册");
        }

        // 3. 构建用户实体并加密密码
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // 昵称可选，未填则默认为用户名
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 4. 保存用户
        sysUserMapper.insert(user);

        // 5. 生成 Token 并返回（注册即登录）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());
    }

    @Override
    public UserInfoResponse getCurrentUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserInfoResponse resp = new UserInfoResponse();
        BeanUtils.copyProperties(user, resp);
        resp.setUserId(user.getId());
        return resp;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("新密码长度不能少于 6 位");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }
}