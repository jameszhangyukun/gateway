package com.zyk.rapid.core.netty.processor;

import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.mpmc.MpmcBlockingQueue;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.context.HttpRequestWrapper;
import com.zyk.rapid.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MPMC的核心实现处理器，最终使用NettyCoreProcessor
 */
public class NettyMpmcProcessor implements NettyProcessor {

    private GatewayConfig gatewayConfig;

    private NettyCoreProcessor nettyCoreProcessor;

    private MpmcBlockingQueue<HttpRequestWrapper> mpmcBlockingQueue;

    private boolean usedExecutorPool;

    private ExecutorService executorService;

    private volatile boolean isRunning = false;

    private Thread consumerProcessor;

    public NettyMpmcProcessor(GatewayConfig gatewayConfig, NettyCoreProcessor nettyCoreProcessor, boolean usedExecutorPool) {
        this.gatewayConfig = gatewayConfig;
        this.nettyCoreProcessor = nettyCoreProcessor;
        this.mpmcBlockingQueue = new MpmcBlockingQueue<>(gatewayConfig.getBufferSize());
        this.usedExecutorPool = usedExecutorPool;
    }

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {
        try {
            System.out.println("Mpmc Put");
            mpmcBlockingQueue.put(httpRequestWrapper);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
        this.nettyCoreProcessor.start();
        if (usedExecutorPool) {
            this.executorService = Executors.newFixedThreadPool(gatewayConfig.getProcessThread());
            for (int i = 0; i < gatewayConfig.getProcessThread(); i++) {
                this.executorService.submit(new ConsumerProcessor());
            }
        } else {
            consumerProcessor = new Thread(new ConsumerProcessor());
            consumerProcessor.start();
        }
    }

    @Override
    public void shutdown() {
        this.isRunning = false;
        this.nettyCoreProcessor.shutdown();
        if (usedExecutorPool) {
            this.executorService.shutdown();
        }
    }

    public class ConsumerProcessor implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                HttpRequestWrapper requestWrapper = null;
                try {
                    requestWrapper = mpmcBlockingQueue.take();
                    nettyCoreProcessor.process(requestWrapper);
                } catch (Exception e) {
                    if (requestWrapper != null) {
                        FullHttpRequest httpRequest = requestWrapper.getFullHttpRequest();
                        ChannelHandlerContext context = requestWrapper.getCtx();
                        FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                        try {
                            if (!HttpUtil.isKeepAlive(httpRequest)) {
                                context.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
                            } else {
                                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                                context.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);

                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}