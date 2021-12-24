package com.zyk.rapid.core.netty.processor;

import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.context.HttpRequestWrapper;

/**
 * NettyBatchEventProcessor
 * flusher缓冲队列的核心实现，最终调用的方法还是使用NettyCoreProcessor
 */
public class NettyBatchEventProcessor implements NettyProcessor{
    public NettyBatchEventProcessor(GatewayConfig gatewayConfig, NettyCoreProcessor nettyCoreProcessor) {

    }

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {

    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }
}
