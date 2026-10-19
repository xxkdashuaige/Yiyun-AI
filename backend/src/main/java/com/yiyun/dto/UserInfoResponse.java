package com.yiyun.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
public class UserInfoResponse {

    private Long userId;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phone;

    private LocalDateTime createdAt;
}