package com.zyk.rapid.core.netty.processor.filter;

import java.lang.annotation.*;

/**
 * 过滤器注解类
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Filter {
    /**
     * 过滤器唯一id
     *
     * @return
     */
    String id();

    /**
     * 过滤器名称
     *
     * @return
     */
    String name() default "";

    ProcessorFilterType value();

    int order() default 0;

}
