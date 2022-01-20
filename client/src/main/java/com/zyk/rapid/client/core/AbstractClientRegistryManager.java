package com.zyk.rapid.client.core;

import com.zyk.gateway.common.config.ServiceDefinition;
import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.constants.RapidConstants;
import com.zyk.gateway.common.util.FastJsonConvertUtil;
import com.zyk.gateway.common.util.ServiceLoader;
import com.zyk.rapid.client.core.autoconfigure.RapidProperties;
import com.zyk.rapid.discovery.api.Registry;
import com.zyk.rapid.discovery.api.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 抽象注册管理器
 */
@Slf4j
public abstract class AbstractClientRegistryManager {
    public static final String PROPERTIES_PATH = "rapid.properties";

    public static final String REGISTRY_ADDRESS_KEY = "registryAddress";

    public static final String NAMESPACE_KEY = "namespace";

    public static final String ENV_KEY = "env";

    protected volatile boolean whetherStart = false;

    public static Properties properties = new Properties();

    public static String registryAddress;

    public static String namespace;

    public static String env;

    protected static String superPath;

    protected static String servicesPath;

    protected static String instancesPath;

    protected static String rulesPath;

    private RegistryService registryService;

    // 静态代码块读取配置文件
    static {
        InputStream is = null;
        is = AbstractClientRegistryManager.class.getClassLoader().getResourceAsStream(PROPERTIES_PATH);
        try {
            if (is != null) {
                properties.load(is);
                registryAddress = properties.getProperty(REGISTRY_ADDRESS_KEY);
                namespace = properties.getProperty(NAMESPACE_KEY);
                env = properties.getProperty(ENV_KEY);
                if (StringUtils.isEmpty(registryAddress)) {
                    log.error("网关配置地址不能为空");
                    throw new RuntimeException("网关配置地址不能为空");
                }
            }
        } catch (Exception e) {
            log.error("#AbstractClientRegistryManager InputStream load is error#", e);

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // ignore
                log.error("#AbstractClientRegistryManager InputStream close is error#", e);
            }
        }
    }

    /**
     * SpringBoot的优先级最高
     *
     * @param rapidProperties
     */
    protected AbstractClientRegistryManager(RapidProperties rapidProperties) throws Exception {
        // 1. 加载配置信息
        if (rapidProperties.getRegistryAddress() != null) {
            registryAddress = rapidProperties.getRegistryAddress();
            env = rapidProperties.getEnv();
            namespace = rapidProperties.getNamespace();
            if (StringUtils.isBlank(namespace)) {
                namespace = RapidProperties.RAPID_PREFIX;
            }
            env = rapidProperties.getEnv();
        }
        //  2. 初始化加载注册中心对象
        ServiceLoader<RegistryService> serviceLoader = ServiceLoader.load(RegistryService.class);
        for (RegistryService service : serviceLoader) {
            service.initialized(registryAddress);
            this.registryService = service;
        }
        // 3. 注册构建顶级目录
        generatorStrutPath(Registry.PATH + namespace + BasicConst.BAR_SEPARATOR + env);

    }

    /**
     * 注册结构目录路径，只需要构建一次
     *
     * @param path
     * @throws Exception
     */
    private void generatorStrutPath(String path) throws Exception {
        superPath = path;
        registryService.registerPathIfNotExists(superPath, "", true);
        registryService.registerPathIfNotExists(servicesPath = superPath + Registry.SERVICE_PREFIX, "", true);
        registryService.registerPathIfNotExists(instancesPath = superPath + Registry.INSTANCE_PREFIX, "", true);
        registryService.registerPathIfNotExists(rulesPath = superPath + Registry.RULE_PREFIX, "", true);
    }

    /**
     * 注册服务定义对象
     *
     * @param serviceDefinition
     * @throws Exception
     */
    protected void registerServiceDefinition(ServiceDefinition serviceDefinition) throws Exception {
        String key = servicesPath + Registry.PATH + serviceDefinition.getUniqueId();
        if (!registryService.isExistKey(key)) {
            String value = FastJsonConvertUtil.convertObjectToJSON(serviceDefinition);
            registryService.registerPathIfNotExists(key, value, true);
        }
    }

    /**
     * 注册服务实例
     *
     * @param serviceInstance
     * @throws Exception
     */
    protected void registerServiceInstance(ServiceInstance serviceInstance) throws Exception {
        String key = instancesPath
                + Registry.PATH
                + serviceInstance.getUniqueId()
                + Registry.PATH
                + serviceInstance.getServiceInstanceId();
        if (!registryService.isExistKey(key)) {
            String value = FastJsonConvertUtil.convertObjectToJSON(serviceInstance);
            registryService.registerPathIfNotExists(key, value, false);
        }
    }

    public boolean isWhetherStart() {
        return whetherStart;
    }

    public static Properties getProperties() {
        return properties;
    }

    public static String getRegistryAddress() {
        return registryAddress;
    }

    public static String getNamespace() {
        return namespace;
    }

    public static String getEnv() {
        return env;
    }

    public static String getSuperPath() {
        return superPath;
    }

    public static String getServicesPath() {
        return servicesPath;
    }

    public static String getInstancesPath() {
        return instancesPath;
    }

    public static String getRulesPath() {
        return rulesPath;
    }
}
