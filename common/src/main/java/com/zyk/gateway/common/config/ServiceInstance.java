package com.zyk.gateway.common.config;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 一个服务对应多个实例
 */
@Data
public class ServiceInstance implements Serializable {
    private static final long serialVersionUID = 9084039783408490207L;

    /**
     * 服务实例id ip:port
     */
    private String serviceInstanceId;

    /**
     * 服务定义唯一id
     */
    private String uniqueId;
    /**
     * 服务实例地址 ip:port
     */
    private String address;
    /**
     * 服务标签信息
     */
    private String tags;
    /**
     * 权重信息
     */
    private Integer weight;
    /**
     * 服务注册时间戳：后面做负载均衡，warmup服务预热
     */
    private long registerTime;
    /**
     * 服务实例启用禁用开关
     */
    private boolean enable = true;
    /**
     * 服务实例的版本号
     */
    private String version;

    public ServiceInstance() {
    }

    public ServiceInstance(String serviceInstanceId, String uniqueId, String address, String tags, Integer weight, long registerTime, boolean enable, String version) {
        this.serviceInstanceId = serviceInstanceId;
        this.uniqueId = uniqueId;
        this.address = address;
        this.tags = tags;
        this.weight = weight;
        this.registerTime = registerTime;
        this.enable = enable;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(serviceInstanceId, that.serviceInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceInstanceId);
    }
}
