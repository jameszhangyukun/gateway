package com.zyk.rapid.core.netty.processor;

import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidNotFoundException;
import com.zyk.gateway.common.exception.RapidPathNoMatchedException;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.rapid.core.context.HttpRequestWrapper;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.helper.RequestHelper;
import com.zyk.rapid.core.helper.ResponseHelper;
import com.zyk.rapid.core.netty.processor.filter.DefaultProcessorFilterFactory;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterFactory;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 核心流程的主执行逻辑
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {
    private ProcessorFilterFactory processorFilterFactory = DefaultProcessorFilterFactory.getInstance();

    @Override
    public void process(HttpRequestWrapper event) {
        ChannelHandlerContext context = event.getCtx();
        FullHttpRequest fullHttpRequest = event.getFullHttpRequest();
        try {
            // 解析http请求，转换为内部对象 Context
            RapidContext rapidContext = RequestHelper.doContext(fullHttpRequest, context);
            // 执行过滤器逻辑Filter Chain
            processorFilterFactory.doFilterChain(rapidContext);
        } catch (RapidPathNoMatchedException e) {
            log.error("#NettyCoreProcessor#processor 网关制定路径未匹配 code：{}, msg{}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(context, fullHttpRequest, httpResponse);
        } catch (RapidNotFoundException e) {
            log.error("#NettyCoreProcessor#processor 网关资源未匹配异常code：{}, msg{}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(context, fullHttpRequest, httpResponse);
        } catch (RapidResponseException e) {
            log.error("#NettyCoreProcessor#processor 网关响应异常code：{}, msg{}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(context, fullHttpRequest, httpResponse);
        } catch (Throwable t) {
            log.error("#NettyCoreProcessor#processor 网关内部未知异常错误", t);
            FullHttpResponse response = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            // 释放资源 写回响应
            doWriteAndRelease(context, fullHttpRequest, response);
        }
    }

    private void doWriteAndRelease(ChannelHandlerContext context, FullHttpRequest request, FullHttpResponse response) {
        context.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE);
        if (!ReferenceCountUtil.release(request)) {
            log.warn("#NettyCoreProcessor# doWriteAndRelease release fail 释放资源失败，request：{}",
                    request.getUri()
            );
        }

    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }
}
