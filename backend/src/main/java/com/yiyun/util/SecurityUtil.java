package com.yiyun.util;

import com.yiyun.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Spring Security 工具类，获取当前登录用户信息。
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未登录返回 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    /**
     * 获取当前登录用户名。
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * 获取当前请求中的 JWT Token（不带 Bearer 前缀）。
     * 用于转发给 ai-service 进行身份验证。
     *
     * @return Token 字符串，没有则返回 null
     */
    public static String getCurrentToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String bearerToken = attrs.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}