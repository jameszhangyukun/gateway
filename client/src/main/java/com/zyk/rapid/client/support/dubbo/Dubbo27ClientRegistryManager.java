package com.zyk.rapid.client.support.dubbo;

import com.zyk.gateway.common.config.ServiceDefinition;
import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.constants.RapidConstants;
import com.zyk.gateway.common.util.NetUtils;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.client.core.AbstractClientRegistryManager;
import com.zyk.rapid.client.core.RapidAnnotationScanner;
import com.zyk.rapid.client.core.autoconfigure.RapidProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class Dubbo27ClientRegistryManager extends AbstractClientRegistryManager implements EnvironmentAware, ApplicationListener<ApplicationEvent> {
    public Dubbo27ClientRegistryManager(RapidProperties rapidProperties) throws Exception {
        super(rapidProperties);
    }

    private Environment environment;

    private static final Set<Object> uniqueBeanSet = new HashSet<>();


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!whetherStart) {
            return;
        }
        if (event instanceof ServiceBeanExportedEvent) {
            ServiceBean<?> serviceBean = ((ServiceBeanExportedEvent) event).getServiceBean();
            try {
                registerServiceBean(serviceBean);
            } catch (Exception e) {
                log.error("Rapid Dubbo 注册服务ServiceBean失败，ServiceBean = {}", serviceBean, e);
            }
        } else if (event instanceof ApplicationStartedEvent) {
            // Start
            System.err.println("**********************************************");
            System.err.println("*************Rapid Dubbo Started  ************");
            System.err.println("**********************************************");
        }
    }

    /**
     * 注册Dubbo服务 获取ServiceBean对象
     *
     * @param serviceBean
     */
    private void registerServiceBean(ServiceBean<?> serviceBean) throws Exception {
        Object bean = serviceBean.getRef();
        if (uniqueBeanSet.add(bean)) {
            ServiceDefinition serviceDefinition = RapidAnnotationScanner.getInstance().scanBuilder(bean, serviceBean);
            if (serviceDefinition != null) {
                // 设置环境
                serviceDefinition.setEnvType(getEnv());
                // 注册服务定义
                registerServiceDefinition(serviceDefinition);

                // 注册服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                String localIp = NetUtils.getLocalIp();
                int port = serviceBean.getProtocol().getPort();
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

    @PostConstruct
    private void init() {
        String port = environment.getProperty(DubboConstants.DUBBO_PROTOCOL_PORT);
        if (StringUtils.isBlank(port)) {
            log.error("Rapid Dubbo服务未启动");
            return;
        }
        whetherStart = true;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
