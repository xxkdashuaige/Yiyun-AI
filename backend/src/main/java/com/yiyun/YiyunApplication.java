package com.yiyun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 遗韵平台后端启动类
 * <p>
 * 负责启动整个 Spring Boot 应用，扫描 MyBatis-Plus 的 Mapper 接口。
 */
@SpringBootApplication
@MapperScan("com.yiyun.mapper") // 扫描所有 Mapper 接口所在包
public class YiyunApplication {
    public static void main(String[] args) {
        SpringApplication.run(YiyunApplication.class, args);
    }
}