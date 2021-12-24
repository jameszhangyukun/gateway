package com.zyk.rapid.core;

/**
 * 网关项目的启动入口
 */
public class Bootstrap {
    public static void main(String[] args) {
        // 1. 加载网关的配置
        GatewayConfig config = GatewayConfigLoader.getInstance().load(args);

        // 2. 插件的初始化工作

        // 3. 初始化服务注册管理中心

        // 4. 启动容器
        GatewayContainer gatewayContainer = new GatewayContainer(config);
        gatewayContainer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(gatewayContainer::shutdown));
    }
}
