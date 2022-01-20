package com.zyk.rapid.client.support.springmvc;

import com.zyk.gateway.common.config.ServiceDefinition;
import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.constants.RapidConstants;
import com.zyk.gateway.common.util.NetUtils;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.client.core.AbstractClientRegistryManager;
import com.zyk.rapid.client.core.RapidAnnotationScanner;
import com.zyk.rapid.client.core.autoconfigure.RapidProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpringMVCClientRegistryManager extends AbstractClientRegistryManager
        implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    ApplicationContext applicationContext;

    @Autowired
    private ServerProperties serverProperties;

    private static final Set<Object> uniqueBeanSet = new HashSet<>();

    public SpringMVCClientRegistryManager(RapidProperties rapidProperties) throws Exception {
        super(rapidProperties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    private void init() {
        // 如果当前验证属性都为空 就进行初始化
        if (!ObjectUtils.allNotNull(serverProperties, serverProperties.getPort())) {
            return;
        }
        super.whetherStart = true;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!whetherStart) {
            return;
        }
        if (event instanceof WebServerInitializedEvent) {
            try {
                registrySpringMVC();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if (event instanceof ApplicationStartedEvent) {
            // Start
            System.err.println("**********************************************");
            System.err.println("*************Rapid SpringMVC Started *********");
            System.err.println("**********************************************");
        }
    }

    private void registrySpringMVC() throws Exception {
        Map<String, RequestMappingHandlerMapping> handlerMappingMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);

        for (RequestMappingHandlerMapping handlerMapping : handlerMappingMap.values()) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                Class<?> beanType = handlerMethod.getBeanType();
                Object bean = applicationContext.getBean(beanType);
                // 如果当前bean已经加载 则不需要加载
                if (uniqueBeanSet.add(bean)) {
                    ServiceDefinition serviceDefinition = RapidAnnotationScanner.getInstance().scanBuilder(bean);
                    if (serviceDefinition != null) {
                        // 设置环境
                        serviceDefinition.setEnvType(getEnv());
                        // 注册服务定义
                        registerServiceDefinition(serviceDefinition);

                        // 注册服务实例
                        ServiceInstance serviceInstance = new ServiceInstance();
                        String localIp = NetUtils.getLocalIp();
                        int port = serverProperties.getPort();
                        String serviceInstanceId = localIp + BasicConst.COLON_SEPARATOR + port;
                        String address = serviceInstanceId;
                        String uniqueId = serviceDefinition.getUniqueId();
                        String version = serviceDefinition.getVersion();

                        serviceInstance.setServiceInstanceId(serviceInstanceId);
                        serviceInstance.setAddress(address);
                        serviceInstance.setWeight(RapidConstants.DEFAULT_WEIGHT);
                        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                        serviceInstance.setVersion(version);
                        serviceInstance.setUniqueId(uniqueId);

                        registerServiceInstance(serviceInstance);

                    }
                }
            }
        }
    }
}
