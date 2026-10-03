package com.yiyun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yiyun.mapper")
public class YinyunApplication {
    public static void main(String[] args) {
        SpringApplication.run(YiyunApplication.class, args);
    }
}