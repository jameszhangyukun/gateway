package com.zyk.rapid.core.netty.processor;

import com.zyk.rapid.core.context.HttpRequestWrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * 核心流程的主执行逻辑
 */
public class NettyCoreProcessor implements NettyProcessor {
    @Override
    public void process(HttpRequestWrapper event) {
        ChannelHandlerContext context = event.getCtx();
        FullHttpRequest fullHttpRequest = event.getFullHttpRequest();
        try {
            // 解析http请求，转换为内部对象 Context

            // 执行过滤器逻辑Filter Chain
            System.out.println("接收到请求");
        }catch (Throwable t){

        }
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }
}
