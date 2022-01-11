package com.zyk.gateway.common.config;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@Data
public class ServiceDefinition implements Serializable {

    private static final long serialVersionUID = -5266773943814111551L;

    /**
     * 唯一的服务id serviceId:version
     */
    private String uniqueId;
    /**
     * 服务id
     */
    private String serviceId;
    /**
     * 服务的版本号
     */
    private String version;
    /**
     * 服务协议
     */
    private String protocol;
    /**
     * 路径匹配规则 访问真实ANT表达式
     */
    private String patternPath;
    /**
     * 环境名称
     */
    private String envType;
    /**
     * 服务启用禁用
     */
    private boolean enable = true;
    /**
     * 服务列表信息 key InvokePath
     * Value ServiceInvoker 方法的描述信息
     */
    private Map<String, ServiceInvoker> invokerMap;

    public ServiceDefinition() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDefinition that = (ServiceDefinition) o;
        return Objects.equals(uniqueId, that.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }
}
