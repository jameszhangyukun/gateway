package com.zyk.gateway.common.config;


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置管理类
 */
public class DynamicConfigManager {
    /**
     * uniqueId表示服务唯一表示
     */
    private ConcurrentHashMap<String, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

    /**
     * 服务实例集合缓存
     */
    private ConcurrentHashMap<String, Set<ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();
    /**
     * 规则集合
     */
    private ConcurrentHashMap<String, Rule> ruleMap = new ConcurrentHashMap<>();

    private DynamicConfigManager() {

    }

    private static class SingletonHolder {
        public static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }

    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public void putServiceDefinition(String uniqueId, ServiceDefinition serviceDefinition) {
        serviceDefinitionMap.put(uniqueId, serviceDefinition);
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }

    public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
        serviceInstances.add(serviceInstance);
    }

    public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> instanceIterator = serviceInstances.iterator();
        while (instanceIterator.hasNext()) {
            ServiceInstance instance = instanceIterator.next();
            if (serviceInstance.getServiceInstanceId().equals(instance.getServiceInstanceId())) {
                instanceIterator.remove();
                break;
            }
        }
        serviceInstances.add(serviceInstance);
    }

    public void removeServiceInstanceId(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> serviceInstances = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> instanceIterator = serviceInstances.iterator();
        while (instanceIterator.hasNext()) {
            ServiceInstance instance = instanceIterator.next();
            if (instance.getServiceInstanceId().equals(serviceInstanceId)) {
                instanceIterator.remove();
                break;
            }
        }
    }

    public void removeServiceInstanceByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }

}
