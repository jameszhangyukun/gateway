package com.zyk.rapid.client.core.autoconfigure;

import com.zyk.rapid.client.support.dubbo.Dubbo27ClientRegistryManager;
import com.zyk.rapid.client.support.springmvc.SpringMVCClientRegistryManager;
import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Servlet;

/**
 * SpringBoot自动装配加载类
 */
@Configuration
@EnableConfigurationProperties(RapidProperties.class)
@ConditionalOnProperty(prefix = RapidProperties.RAPID_PREFIX, name = {"registryAddress", "namespace"})
public class RapidClientAutoConfiguration {
    @Autowired
    private RapidProperties rapidProperties;

    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean(SpringMVCClientRegistryManager.class)
    public SpringMVCClientRegistryManager springMVCClientRegistryManager() throws Exception {
        return new SpringMVCClientRegistryManager(rapidProperties);
    }

    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean(Dubbo27ClientRegistryManager.class)
    public Dubbo27ClientRegistryManager dubbo27ClientRegistryManager() throws Exception {
        return new Dubbo27ClientRegistryManager(rapidProperties);
    }
}
