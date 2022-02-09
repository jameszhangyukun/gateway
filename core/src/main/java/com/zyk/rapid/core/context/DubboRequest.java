package com.zyk.rapid.core.context;

import lombok.Getter;
import lombok.Setter;

/**
 * Dubbo Request
 */
@Getter
@Setter
public class DubboRequest {
    /**
     * 注册中心地址
     */
    private String registriesStr;
    /**
     * dubbo接口名称
     */
    private String interfaceClass;
    /**
     * dubbo 服务名称
     */
    private String methodName;
    /**
     * 参数类型
     */
    private String[] parameterTypes;
    /**
     * 调用参数内容
     */
    private Object[] args;
    /**
     * 超时时间
     */
    private int timeout;
    /**
     * 版本号
     */
    private String version;
}
