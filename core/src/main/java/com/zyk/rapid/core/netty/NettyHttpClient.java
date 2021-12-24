package com.zyk.rapid.core.netty;

import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.LifeCycle;
import com.zyk.rapid.core.helper.AsyncHttpHelper;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;

/**
 * HTTP客户端启动类，主要用于下游服务的请求转发
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {

    private AsyncHttpClient asyncHttpClient;

    private DefaultAsyncHttpClientConfig.Builder clientBuilder;

    private GatewayConfig gatewayConfig;

    private EventLoopGroup eventLoopGroupWork;

    public NettyHttpClient(GatewayConfig gatewayConfig, EventLoopGroup eventLoopGroupWork) {
        this.gatewayConfig = gatewayConfig;
        this.eventLoopGroupWork = eventLoopGroupWork;
        init();
    }


    @Override
    public void init() {
        this.clientBuilder = new DefaultAsyncHttpClientConfig.Builder()
                .setFollowRedirect(false)
                .setEventLoopGroup(eventLoopGroupWork)
                .setConnectTimeout(gatewayConfig.getHttpConnectTime())
                .setRequestTimeout(gatewayConfig.getHttpRequestTimeout())
                .setMaxRequestRetry(gatewayConfig.getHttpMaxRequestRetry())
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                .setCompressionEnforced(true)
                .setMaxConnections(gatewayConfig.getHttpMaxConnections())
                .setMaxConnectionsPerHost(gatewayConfig.getHttpConnectionPerHost())
                .setPooledConnectionIdleTimeout(gatewayConfig.getHttpPooledConnectionIdleTimeout());
    }

    @Override
    public void start() {
        this.asyncHttpClient = new DefaultAsyncHttpClient(clientBuilder.build());
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);

    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                this.asyncHttpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
