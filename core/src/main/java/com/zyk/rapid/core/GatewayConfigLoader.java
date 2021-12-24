package com.zyk.rapid.core;

import com.zyk.gateway.common.util.PropertiesUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * 网关配置信息加载类
 * 网关配置加载的规则：优先级
 * 1. 运行参数
 * 2. jvm参数
 * 3. 环境变量
 * 4. 配置文件
 * 5. 内部GatewayConfig对象的默认属性值
 */
@Slf4j
public class GatewayConfigLoader {

    private final static String CONFIG_ENV_PREFIX = "GATEWAY_";

    private final static String CONFIG_JVM_PREFIX = "gateway.";

    private final static String CONFIG_FILE = "gateway.properties";

    private final static GatewayConfigLoader INSTANCE = new GatewayConfigLoader();

    private GatewayConfig gatewayConfig = new GatewayConfig();

    private GatewayConfigLoader() {

    }

    public static GatewayConfigLoader getInstance() {
        return INSTANCE;
    }

    public static GatewayConfig getGatewayConfig() {
        return INSTANCE.gatewayConfig;
    }

    public GatewayConfig load(String args[]) {
        // 1. 加载配置文件
        {
            InputStream is = GatewayConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (is != null) {
                try {
                    Properties properties = new Properties();
                    properties.load(is);
                    PropertiesUtils.properties2Object(properties, gatewayConfig);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException ignored) {

                    }
                }
            }
        }

        // 2. 环境变量
        {
            Map<String, String> env = System.getenv();
            Properties properties = new Properties();
            properties.putAll(env);
            PropertiesUtils.properties2Object(properties, gatewayConfig);
        }

        // 3, Jvm参数
        {
            Properties properties = System.getProperties();
            PropertiesUtils.properties2Object(properties, gatewayConfig, CONFIG_JVM_PREFIX);
        }
        // 4. 运行参数
        {
            if (args != null && args.length > 0) {
                Properties properties = new Properties();
                for (String arg : args) {
                    if (arg.startsWith("--") && arg.contains("=")) {
                        properties.put(arg.substring(2, arg.indexOf("=")), arg.substring(arg.indexOf("=") + 1));
                    }
                }
                PropertiesUtils.properties2Object(properties, gatewayConfig);
            }
        }
        return gatewayConfig;
    }
}
