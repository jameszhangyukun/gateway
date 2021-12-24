package com.zyk.rapid.core.netty.processor;

import com.zyk.rapid.core.context.HttpRequestWrapper;

/**
 * 处理Netty核心逻辑的执行器接口
 */
public interface NettyProcessor {
    /**
     * process
     *
     * @param httpRequestWrapper
     */
    void process(HttpRequestWrapper httpRequestWrapper);

    /**
     * start
     */
    void start();

    /**
     * 执行器资源释放/关闭方法
     */
    void shutdown();
}
