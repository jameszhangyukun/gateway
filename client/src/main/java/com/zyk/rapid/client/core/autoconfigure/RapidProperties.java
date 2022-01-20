package com.zyk.rapid.client.core.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.zyk.rapid.client.core.autoconfigure.RapidProperties.RAPID_PREFIX;

/**
 * 配置类
 */
@Data
@ConfigurationProperties(prefix = RAPID_PREFIX)
public class RapidProperties {
    public static final String RAPID_PREFIX = "rapid";
    /**
     * etcd注册中心
     */
    private String registryAddress;
    /**
     * 命名空间
     */
    private String namespace = RAPID_PREFIX;

    private String env = "dev";

}
