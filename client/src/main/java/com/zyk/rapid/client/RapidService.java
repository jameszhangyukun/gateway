package com.zyk.rapid.client;

import java.lang.annotation.*;

/**
 * RUNTIME SOURCE
 * 服务注解类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RapidService {
    /**
     * 服务Id
     *
     * @return 服务唯一Id
     */
    String serviceId();

    /**
     * 服务版本好
     *
     * @return 服务版本
     */
    String version() default "1.0.0";

    /**
     * 协议类型
     *
     * @return 协议类型
     */
    RapidProtocol protocol();

    /**
     * ANT路径匹配规则
     *
     * @return ANT路径匹配规则
     */
    String patternPath();
}

