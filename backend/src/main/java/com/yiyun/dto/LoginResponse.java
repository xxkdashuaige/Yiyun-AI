package com.yiyun.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应数据
 * <p>
 * 登录成功后返回 Token 和用户基本信息给前端。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** JWT Token，后续请求需放在 Authorization 请求头 */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatarUrl;
}