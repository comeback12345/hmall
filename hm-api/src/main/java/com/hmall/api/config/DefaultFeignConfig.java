package com.hmall.api.config;

import com.hmall.api.client.fallback.PayClientFallback;
import com.hmall.api.client.fallback.ItemClientFallbackFactory;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DefaultFeignConfig.class);

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Long userId= UserContext.getUser();
                if (userId != null) {
                    template.header("user-info",userId.toString() );
                }
            }
        };
    }

    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory() {
        return new ItemClientFallbackFactory();
    }
    @Bean
    public PayClientFallback payClientFallbackFactory() {
        return new PayClientFallback();
    }
}
