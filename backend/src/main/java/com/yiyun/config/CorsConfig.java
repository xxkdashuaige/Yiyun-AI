package com.yiyun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域（CORS）配置
 * <p>
 * 允许前端（如 Vue 开发服务器）跨域访问后端接口。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);                    // 允许携带 Cookie/认证信息
        config.addAllowedOriginPattern("*");                 // 允许所有来源
        config.addAllowedHeader("*");                        // 允许所有请求头
        config.addAllowedMethod("*");                        // 允许所有请求方法
        config.setMaxAge(3600L);                              // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);      // 对所有路径生效
        return new CorsFilter(source);
    }
}