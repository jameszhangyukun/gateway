package com.zyk.rapid.core.netty.processor.filter;

import lombok.Data;

/**
 * 所有的过滤器配置实现的父类
 */
@Data
public class FilterConfig {
    /**
     * 是否打印日志
     */
    private boolean loggable = false;
}
