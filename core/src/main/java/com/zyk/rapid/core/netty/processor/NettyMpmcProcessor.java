package com.zyk.rapid.core.netty.processor;

import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.context.HttpRequestWrapper;

/**
 * MPMC的核心实现处理器，最终使用NettyCoreProcessor
 */
public class NettyMpmcProcessor implements NettyProcessor {

    private GatewayConfig gatewayConfig;

    private NettyCoreProcessor nettyCoreProcessor;


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
