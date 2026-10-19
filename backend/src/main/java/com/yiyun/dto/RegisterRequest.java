package com.yiyun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 * <p>
 * 接收前端传来的用户名、密码、确认密码和昵称，使用 JSR-303 校验。
 */
@Data
public class RegisterRequest {

    /** 用户名（必填，4-20位字母数字下划线） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在 4-20 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 密码（必填，6-20位） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;

    /** 确认密码（必填，需与密码一致） */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** 昵称（可选，不超过20位） */
    @Size(max = 20, message = "昵称长度不能超过 20 位")
    private String nickname;
}