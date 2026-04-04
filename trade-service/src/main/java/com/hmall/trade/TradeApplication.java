package com.hmall.trade;

import com.hmall.api.config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan(basePackages = "com.hmall.trade.mapper")
@SpringBootApplication
@EnableFeignClients(basePackages = "com.hmall.api.client", defaultConfiguration = DefaultFeignConfig.class)
public class TradeApplication {
    public static void main(String[] args) {
        // 设置 Feign 客户端懒加载，避免启动时立即创建
        System.setProperty("feign.client.config.default.connectTimeout", "5000");
        System.setProperty("feign.client.config.default.readTimeout", "5000");
        SpringApplication.run(TradeApplication.class, args);
    }
}