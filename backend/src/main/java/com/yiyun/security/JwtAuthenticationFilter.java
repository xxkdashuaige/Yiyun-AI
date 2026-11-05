package com.yiyun.security;

import com.yiyun.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 对每个请求检查 Authorization 请求头中的 Token。
 * 如果 Token 有效，将用户认证信息存入 SecurityContext，
 * 后续的接口就能识别当前登录用户。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsService userDetailsService,
                                   @Lazy SecurityContextRepository securityContextRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * 过滤逻辑：从请求头提取 Token，校验后设置认证上下文
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // Token 有效，获取用户名并加载用户信息
            String username = jwtUtil.parseToken(token).get("username", String.class);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // 构造认证对象并存入 SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // 保存到 SecurityContextRepository，确保异步分派（如 SSE）时能恢复认证信息
            securityContextRepository.saveContext(context, request, response);
        }
        // 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token
     * <p>
     * 请求头格式：Authorization: Bearer <token>
     *
     * @return Token 字符串，没有则返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        return null;
    }
}