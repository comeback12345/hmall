package com.hmall.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "com.hmall.item.mapper")
@SpringBootApplication(exclude = {
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
public class ItemApplication {
    public static void main(String[] args) {
        // 启用 Bean 覆盖和延迟初始化，避免 Seata 启动问题
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(ItemApplication.class, args);
    }
}