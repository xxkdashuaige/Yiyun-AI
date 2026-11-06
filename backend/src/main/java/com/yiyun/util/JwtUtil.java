package com.yiyun.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 负责 Token 的生成、解析与校验。
 * Token 中存储了用户 ID 和用户名，用于无状态身份认证。
 */
@Component
public class JwtUtil {

    /** JWT 签名密钥（从配置文件读取） */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 过期时间（毫秒，默认 24 小时） */
    @Value("${jwt.expiration}")
    private long expiration;

    /** 根据 secret 生成 HMAC 签名密钥 */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(userId.toString())          // subject 存储用户 ID
                .claim("username", username)         // 自定义 claim 存储用户名
                .issuedAt(now)                       // 签发时间
                .expiration(expiryDate)              // 过期时间
                .signWith(getSigningKey(), Jwts.SIG.HS256)  // 显式指定 HS256，与 ai-service 保持一致
                .compact();
    }

    /** 解析 Token，返回 Claims（负载内容） */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 校验 Token 是否有效（签名正确且未过期） */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 从 Token 中获取用户 ID */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
}