package com.zyk.rapid.core.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import com.zyk.gateway.common.concurrent.ParallelFlusher;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.context.HttpRequestWrapper;
import com.zyk.rapid.core.helper.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

/**
 * NettyBatchEventProcessor
 * flusher缓冲队列的核心实现，最终调用的方法还是使用NettyCoreProcessor
 */
public class NettyBatchEventProcessor implements NettyProcessor {

    private static final String THREAD_NAME_PREFIX = "gateway-flusher-";

    private GatewayConfig gatewayConfig;

    private NettyCoreProcessor nettyCoreProcessor;

    private ParallelFlusher<HttpRequestWrapper> parallelFlusher;

    public NettyBatchEventProcessor(GatewayConfig gatewayConfig, NettyCoreProcessor nettyCoreProcessor) {
        this.gatewayConfig = gatewayConfig;
        this.nettyCoreProcessor = nettyCoreProcessor;
        BatchEventProcessorListener processorListener = new BatchEventProcessorListener();
        ParallelFlusher.Builder<HttpRequestWrapper> builder = new ParallelFlusher.Builder<HttpRequestWrapper>()
                .setBufferSize(gatewayConfig.getBufferSize())
                .setThreads(gatewayConfig.getProcessThread())
                .setNamePrefix(ProducerType.MULTI.name())
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setWaitStrategy(gatewayConfig.getATrueWaitStrategy());
        builder.setEventListener(processorListener);
        this.parallelFlusher = builder.build();
    }

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {
        this.parallelFlusher.add(httpRequestWrapper);
    }

    @Override
    public void start() {
        this.nettyCoreProcessor.start();
        this.parallelFlusher.start();
    }

    @Override
    public void shutdown() {
        this.nettyCoreProcessor.shutdown();
        this.parallelFlusher.shutdown();
    }

    public class BatchEventProcessorListener implements ParallelFlusher.EventListener<HttpRequestWrapper> {

        @Override
        public void onEvent(HttpRequestWrapper event) throws Exception {
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable t, long sequence, HttpRequestWrapper event) {
            FullHttpRequest httpRequest = event.getFullHttpRequest();
            ChannelHandlerContext context = event.getCtx();
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            try {
                if (!HttpUtil.isKeepAlive(httpRequest)) {
                    context.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    context.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
