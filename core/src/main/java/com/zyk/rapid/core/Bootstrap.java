package com.zyk.rapid.core;

import com.zyk.rapid.core.discovery.RegistryManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 网关项目的启动入口
 */
@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        // 1. 加载网关的配置
        GatewayConfig config = GatewayConfigLoader.getInstance().load(args);

        // 2. 插件的初始化工作

        // 3. 初始化服务注册管理中心
        try {
            RegistryManager.getInstance().initialize(config);
        } catch (Exception e) {
            log.error("RegistryManager is failed", e);
        }
        // 4. 启动容器
        GatewayContainer gatewayContainer = new GatewayContainer(config);
        gatewayContainer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(gatewayContainer::shutdown));
    }
}
