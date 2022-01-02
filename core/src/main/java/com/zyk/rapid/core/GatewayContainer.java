package com.zyk.rapid.core;

import com.zyk.gateway.common.constants.RapidBufferHelper;
import com.zyk.rapid.core.netty.NettyHttpClient;
import com.zyk.rapid.core.netty.NettyHttpServer;
import com.zyk.rapid.core.netty.processor.NettyBatchEventProcessor;
import com.zyk.rapid.core.netty.processor.NettyCoreProcessor;
import com.zyk.rapid.core.netty.processor.NettyMpmcProcessor;
import com.zyk.rapid.core.netty.processor.NettyProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * 主流程的容器
 */
@Slf4j
public class GatewayContainer implements LifeCycle {
    /**
     * 核心配置类
     */
    private final GatewayConfig gatewayConfig;
    /**
     * 接收http请求的server
     */
    private NettyHttpServer nettyHttpServer;
    /**
     * http转发的核心类
     */
    private NettyHttpClient nettyHttpClient;
    /**
     * 核心处理器
     */
    private NettyProcessor nettyProcessor;

    public GatewayContainer(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
        init();
    }


    @Override
    public void init() {
        // 1. 构建核心处理器
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();

        // 2. 开启缓存
        String bufferType = gatewayConfig.getBufferType();

        if (RapidBufferHelper.isFlusher(bufferType)) {
            nettyProcessor = new NettyBatchEventProcessor(gatewayConfig, nettyCoreProcessor);
        } else if (RapidBufferHelper.isMpmc(bufferType)) {
            nettyProcessor = new NettyMpmcProcessor(gatewayConfig, nettyCoreProcessor,true);
        } else {
            nettyProcessor = nettyCoreProcessor;
        }
        // 3. 创建NettyHttpServer
        nettyHttpServer = new NettyHttpServer(gatewayConfig, nettyProcessor);
        // 4. 创建NettyHttpClient
        nettyHttpClient = new NettyHttpClient(gatewayConfig, nettyHttpServer.getEventLoopGroupWork());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("RapidContainer started !");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutdown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
