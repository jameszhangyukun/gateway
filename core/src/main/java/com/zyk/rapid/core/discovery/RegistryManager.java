package com.zyk.rapid.core.discovery;

import com.alibaba.fastjson.JSONObject;
import com.zyk.gateway.common.config.*;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.util.FastJsonConvertUtil;
import com.zyk.gateway.common.util.Pair;
import com.zyk.gateway.common.util.ServiceLoader;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.discovery.api.Notify;
import com.zyk.rapid.discovery.api.Registry;
import com.zyk.rapid.discovery.api.RegistryService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.zyk.gateway.common.constants.RapidProtocol.DUBBO;
import static com.zyk.gateway.common.constants.RapidProtocol.HTTP;

/**
 * 注册中心管理类
 */
@Slf4j
public class RegistryManager {
    private RegistryService registryService;
    private static String superPath;

    private static String servicesPath;

    private static String instancesPath;

    private static String rulesPath;

    private static String gatewayPath;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private GatewayConfig gatewayConfig;

    private RegistryManager() {

    }

    public void initialize(GatewayConfig gatewayConfig) throws Exception {
        // 路径设置
        superPath = Registry.PATH + gatewayConfig.getNamespace() + BasicConst.BAR_SEPARATOR + gatewayConfig.getEnv();
        servicesPath = superPath + Registry.SERVICE_PREFIX;
        instancesPath = superPath + Registry.INSTANCE_PREFIX;
        rulesPath = superPath + Registry.RULE_PREFIX;
        gatewayPath = superPath + Registry.GATEWAY_PREFIX;
        this.gatewayConfig = gatewayConfig;

        // 2. 加载注册中心对象
        ServiceLoader<RegistryService> registryServices = ServiceLoader.load(RegistryService.class);
        for (RegistryService service : registryServices) {
            service.initialized(gatewayConfig.getRegistryAddress());
            this.registryService = service;
        }
        // 3.注册监听
        this.registryService.addWatcherListeners(superPath, new ServiceListener());

        // 4. 订阅服务
        subscribeService();

        // 注册网关服务
        RegistryServer registryServer = new RegistryServer(registryService);
        registryServer.registerSelf();
    }

    /**
     * 订阅服务：拉取etcd注册中心中所有需要使用的元数据信息，解析并放置到缓存中
     */
    private synchronized void subscribeService() {
        log.info("===== #RegistryManager# subscribe service start =====");
        try {
            // 1. 加载服务定义和服务实例集合：获取ServicePath下的所有服务
            List<Pair<String, String>> definitionList = this.registryService.getListByPrefixKey(servicesPath);
            for (Pair<String, String> definition : definitionList) {
                String definitionPath = definition.getObject1();
                String definitionJson = definition.getObject2();
                // 排除根目录
                if (definitionPath.equals(servicesPath)) {
                    continue;
                }
                String uniqueId = definitionPath.substring(servicesPath.length() + 1);
                ServiceDefinition serviceDefinition = parseServiceDefinition(definitionJson);
                DynamicConfigManager.getInstance().putServiceDefinition(uniqueId, serviceDefinition);
                log.info("RegistryManager subscribeService load service definition uniqueId:{},serviceDefinition:{}",
                        uniqueId,
                        serviceDefinition);
                // 2. 加载服务实例集合
                String serviceInstancePrefix = instancesPath + Registry.PATH + uniqueId;
                HashSet<ServiceInstance> serviceInstanceSet = new HashSet<>();
                List<Pair<String, String>> instanceList = this.getRegistryService().getListByPrefixKey(serviceInstancePrefix);
                for (Pair<String, String> instance : instanceList) {
                    String instancePath = instance.getObject1();
                    String instanceJson = instance.getObject2();

                    ServiceInstance serviceInstance = FastJsonConvertUtil.convertJSONToObject(instanceJson, ServiceInstance.class);
                    serviceInstanceSet.add(serviceInstance);
                }
                DynamicConfigManager.getInstance().addServiceInstance(uniqueId, serviceInstanceSet);
                log.info("RegistryManager subscribeService load service instance uniqueId:{},serviceDefinition:{}",
                        uniqueId,
                        serviceInstanceSet);
            }
            // 2. 加载规则集合
            List<Pair<String, String>> ruleList = this.registryService.getListByPrefixKey(rulesPath);
            for (Pair<String, String> r : ruleList) {
                String ruleUrl = r.getObject1();
                String ruleJson = r.getObject2();
                if (ruleUrl.equals(rulesPath)) {
                    continue;
                }
                Rule rule = FastJsonConvertUtil.convertJSONToObject(ruleJson, Rule.class);
                DynamicConfigManager.getInstance().putRule(rule.getId(), rule);
                log.info("RegistryManager subscribeService load service instance ruleId:{},Rule:{}",
                        rule.getId(),
                        rule);
            }
        } catch (Exception e) {
            log.info("subscribe service failed", e);
        } finally {
            countDownLatch.countDown();
            log.info("====== #RegistryManager# subscribe service end ======");
        }


    }

    /**
     * 把从注册中心拉取的服务定义信息转换为ServiceDefinition
     *
     * @param definitionJson
     * @return
     */
    private ServiceDefinition parseServiceDefinition(String definitionJson) {
        Map<String, Object> jsonMap = FastJsonConvertUtil.convertJSONToObject(definitionJson, Map.class);
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        // 填充serviceDefinition
        serviceDefinition.setUniqueId((String) jsonMap.get("uniqueId"));
        serviceDefinition.setServiceId((String) jsonMap.get("serviceId"));
        serviceDefinition.setProtocol((String) jsonMap.get("protocol"));
        serviceDefinition.setVersion((String) jsonMap.get("version"));
        serviceDefinition.setPatternPath((String) jsonMap.get("patternPath"));
        serviceDefinition.setEnvType((String) jsonMap.get("envType"));

        JSONObject jsonInvokerMap = (JSONObject) jsonMap.get("invokerMap");
        Map<String, ServiceInvoker> invokerMap = new HashMap<>();
        switch (serviceDefinition.getProtocol()) {
            case DUBBO:
                Map<String, Object> dubboInvokerMap = FastJsonConvertUtil.convertJSONToObject(jsonInvokerMap, Map.class);
                for (Map.Entry<String, Object> entry : dubboInvokerMap.entrySet()) {
                    String path = entry.getKey();
                    JSONObject jsonInvoker = (JSONObject) entry.getValue();
                    DubboServiceInvoker dubboServiceInvoker = FastJsonConvertUtil.convertJSONToObject(jsonInvoker, DubboServiceInvoker.class);
                    invokerMap.put(path, dubboServiceInvoker);
                }
                break;
            case HTTP:
                Map<String, Object> httpInvokerMap = FastJsonConvertUtil.convertJSONToObject(jsonInvokerMap, Map.class);
                for (Map.Entry<String, Object> entry : httpInvokerMap.entrySet()) {
                    String path = entry.getKey();
                    JSONObject jsonInvoker = (JSONObject) entry.getValue();
                    HttpServiceInvoker httpServiceInvoker = FastJsonConvertUtil.convertJSONToObject(jsonInvoker, HttpServiceInvoker.class);
                    invokerMap.put(path, httpServiceInvoker);
                }
                break;
            default:
                break;
        }


        serviceDefinition.setInvokerMap(invokerMap);
        return serviceDefinition;
    }

    public RegistryService getRegistryService() {
        return registryService;
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

    public static String getGatewayPath() {
        return gatewayPath;
    }

    class RegistryServer {
        private RegistryService registryService;

        private String selfPath;

        public RegistryServer(RegistryService registryService) throws Exception {
            this.registryService = registryService;
            this.registryService.registerPathIfNotExists(superPath, "", true);
            this.registryService.registerPathIfNotExists(gatewayPath, "", true);
            this.selfPath = gatewayPath + Registry.PATH + gatewayConfig.getGatewayId();
        }

        public void registerSelf() throws Exception {
            String s = FastJsonConvertUtil.convertObjectToJSON(gatewayConfig);
            this.registryService.registerPathIfNotExists(selfPath, s, false);
        }
    }

    class ServiceListener implements Notify {
        @Override
        public void put(String key, String value) throws Exception {
            countDownLatch.await();
            if (servicesPath.equals(key) || instancesPath.equals(key) || rulesPath.equals(key)) {
                return;
            }
            // 服务定义变更
            if (key.contains(servicesPath)) {
                String uniqueId = key.substring(servicesPath.length() + 1);
                ServiceDefinition serviceDefinition = parseServiceDefinition(value);
                DynamicConfigManager.getInstance().putServiceDefinition(uniqueId, serviceDefinition);
                return;
            }
            // 服务实例发生变更
            if (key.contains(instancesPath)) {
                String temp = key.substring(instancesPath.length() + 1);
                String[] split = temp.split(Registry.PATH);
                if (split.length == 2) {
                    String uniqueId = split[0];
                    ServiceInstance serviceInstance = FastJsonConvertUtil.convertJSONToObject(value, ServiceInstance.class);
                    DynamicConfigManager.getInstance().updateServiceInstance(uniqueId, serviceInstance);
                }
                return;
            }
            if (key.contains(rulesPath)) {
                String ruleId = key.substring(rulesPath.length() + 1);
                Rule rule = FastJsonConvertUtil.convertJSONToObject(value, Rule.class);
                DynamicConfigManager.getInstance().putRule(ruleId, rule);
            }
        }

        @Override
        public void delete(String key) throws Exception {
            countDownLatch.await();
            if (servicesPath.equals(key) || instancesPath.equals(key) || rulesPath.equals(key)) {
                return;
            }
            // 服务定义变更
            if (key.contains(servicesPath)) {
                String uniqueId = key.substring(servicesPath.length() + 1);
                DynamicConfigManager.getInstance().removeServiceDefinition(uniqueId);
                DynamicConfigManager.getInstance().removeServiceInstanceByUniqueId(uniqueId);
                return;
            }
            // 服务实例发生变更
            if (key.contains(instancesPath)) {
                String temp = key.substring(instancesPath.length() + 1);
                String[] split = temp.split(Registry.PATH);
                if (split.length == 2) {
                    String uniqueId = split[0];
                    String serviceInstanceId = split[1];
                    DynamicConfigManager.getInstance().removeServiceInstanceId(uniqueId, serviceInstanceId);
                }
                return;
            }
            if (key.contains(rulesPath)) {
                String ruleId = key.substring(rulesPath.length() + 1);
                DynamicConfigManager.getInstance().removeRule(ruleId);
            }
        }
    }

    static class RegistryManagerSingletonHolder {
        private static final RegistryManager INSTANCE = new RegistryManager();
    }

    public static RegistryManager getInstance() {
        return RegistryManagerSingletonHolder.INSTANCE;
    }


}
