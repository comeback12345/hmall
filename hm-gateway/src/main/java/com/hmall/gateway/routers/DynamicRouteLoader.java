package com.hmall.gateway.routers;


import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;

    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";

    private final Set<String> routeIDs = new HashSet<>();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        //项目启动就拉取配置，并且添加配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String s) {
                        //2.监听到配置变更就更新路由表
                        updateConfigInfo(s);
                    }
                });
        //3第一次读取配置也需要更新路由表
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo) {
        //1.解析配置信息，转为
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo,RouteDefinition.class);
        //2.删除旧的路由表
        for (String routeID : routeIDs) {
            writer.delete(Mono.just(routeID)).subscribe();//做一个订阅
        }
        routeIDs.clear();
        //3更新路由表
        for (RouteDefinition routeDefinition : routeDefinitions) {
            //3.1更新新的路由表
            writer.save(Mono.just(routeDefinition)).subscribe();
            //3.2记录路由id，方便删除
            routeIDs.add(routeDefinition.getId());
        }


    }
}
