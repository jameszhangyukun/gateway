package com.zyk.rapid.client;

import java.lang.annotation.*;

/**
 * 必须在服务方法上强制声明
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RapidInvoker {
    /**
     * 访问路径
     *
     * @return 访问路径
     */
    String path();

}
