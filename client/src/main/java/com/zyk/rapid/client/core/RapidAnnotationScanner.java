package com.zyk.rapid.client.core;

import com.zyk.gateway.common.config.DubboServiceInvoker;
import com.zyk.gateway.common.config.HttpServiceInvoker;
import com.zyk.gateway.common.config.ServiceDefinition;
import com.zyk.gateway.common.config.ServiceInvoker;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.rapid.client.RapidInvoker;
import com.zyk.rapid.client.RapidProtocol;
import com.zyk.rapid.client.RapidService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ServiceBean;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.zyk.rapid.client.support.dubbo.DubboConstants.DUBBO_TIMEOUT;

/**
 * 注解扫描类 扫描 @RapidService 和 RapidInvoker
 */
public class RapidAnnotationScanner {
    private RapidAnnotationScanner() {

    }

    private static class SingletonHolder {
        private static final RapidAnnotationScanner INSTANCE = new RapidAnnotationScanner();
    }

    public static RapidAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 扫描传入的bean对象，返回ServiceDefinition
     *
     * @param bean 扫描对象
     * @return ServiceDefinition
     */
    public synchronized ServiceDefinition scanBuilder(Object bean, Object... args) {
        Class<?> beanClass = bean.getClass();
        if (beanClass.isAnnotationPresent(RapidService.class)) {
            RapidService rapidService = beanClass.getAnnotation(RapidService.class);
            String serviceId = rapidService.serviceId();
            RapidProtocol protocol = rapidService.protocol();
            String patternPath = rapidService.patternPath();
            String version = rapidService.version();

            ServiceDefinition serviceDefinition = new ServiceDefinition();
            Map<String, ServiceInvoker> serviceInvokerMap = new HashMap<>();

            for (Method method : beanClass.getMethods()) {
                if (method.isAnnotationPresent(RapidInvoker.class)) {
                    RapidInvoker rapidInvoker = method.getAnnotation(RapidInvoker.class);
                    String path = rapidInvoker.path();
                    switch (protocol) {
                        case HTTP:
                            HttpServiceInvoker serviceInvoker = createHttpServiceInvoker(path, bean, method);
                            serviceInvokerMap.put(path, serviceInvoker);
                            break;
                        case DUBBO:
                            ServiceBean<?> serviceBean = (ServiceBean<?>) args[0];
                            DubboServiceInvoker dubboServiceInvoker = createDubboServiceInvoker(path, serviceBean, method);
                            serviceInvokerMap.put(path, dubboServiceInvoker);
                            String dubboVersion = dubboServiceInvoker.getVersion();
                            if (StringUtils.isNoneBlank(dubboVersion)) {
                                version = dubboVersion;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setProtocol(protocol.getCode());
            serviceDefinition.setEnable(true);
            serviceDefinition.setInvokerMap(serviceInvokerMap);
            serviceDefinition.setPatternPath(patternPath);
            serviceDefinition.setVersion(version);
            return serviceDefinition;
        }
        return null;
    }

    /**
     * 构建DubboServiceInvoker对象
     *
     * @param path
     * @param bean
     * @param method
     * @return
     */
    private DubboServiceInvoker createDubboServiceInvoker(String path, ServiceBean<?> bean, Method method) {
        DubboServiceInvoker dubboServiceInvoker = new DubboServiceInvoker();
        dubboServiceInvoker.setInvokerPath(path);
        dubboServiceInvoker.setMethodName(method.getName());
        dubboServiceInvoker.setRegisterAddress(bean.getRegistry().getAddress());
        dubboServiceInvoker.setInterfaceClass(bean.getInterface());
        String[] parameterTypes = new String[method.getParameterCount()];
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            parameterTypes[i] = method.getParameterTypes()[i].getName();
        }
        dubboServiceInvoker.setParameterTypes(parameterTypes);
        Integer timeout = bean.getTimeout();
        if (timeout == null || timeout == 0) {
            ProviderConfig provider = bean.getProvider();
            if (provider != null) {
                Integer providerTimeout = provider.getTimeout();
                if (providerTimeout == null || providerTimeout == 0) {
                    timeout = DUBBO_TIMEOUT;
                } else {
                    timeout = providerTimeout;
                }
            }
        }
        dubboServiceInvoker.setTimeout(timeout);
        dubboServiceInvoker.setVersion(bean.getVersion());
        return dubboServiceInvoker;
    }

    /**
     * 构建HttpServiceInvoker对象
     *
     * @param path
     * @param bean
     * @param method
     * @return
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path, Object bean, Method method) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }
}
