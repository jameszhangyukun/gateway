package com.zyk.rapid.core.helper;

import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.context.RapidResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.ObjectUtil;
import org.asynchttpclient.Response;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ResponseHelper {
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode) {
        RapidResponse rapidResponse = RapidResponse.buildRapidResponse(responseCode);
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                , HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(rapidResponse.getContent().getBytes(StandardCharsets.UTF_8)));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }

    public static void writeResponse(Context context) {
        context.releaseRequest();
        if (context.isWrittened()) {
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(context, (RapidResponse) context.getResponse());
            if (!context.isKeepAlive()) {
                ChannelHandlerContext contextNettyCtx = context.getNettyCtx();
                contextNettyCtx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            } else {
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                context.getNettyCtx().writeAndFlush(httpResponse);
            }

            //设置写回状态为complete
            context.completed();
        } else if (context.isCompleted()) {
            context.invokeCompletedCallback();
        }
    }

    private static FullHttpResponse getHttpResponse(Context context, RapidResponse response) {
        ByteBuf content;
        if (Objects.nonNull(response.getFutureResponse())) {
            content = Unpooled.wrappedBuffer(response.getFutureResponse().getResponseBodyAsByteBuffer());
        } else if (response.getContent() != null) {
            content = Unpooled.wrappedBuffer(response.getContent().getBytes(StandardCharsets.UTF_8));
        } else {
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes(StandardCharsets.UTF_8));
        }
        if (Objects.isNull(response.getFutureResponse())) {
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                    , response.getHttpResponseStatus(),
                    content);

            httpResponse.headers().add(response.getResponseHeaders());
            httpResponse.headers().add(response.getExtraResponseHeaders());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            return httpResponse;
        } else {
            response.getFutureResponse().getHeaders().add(response.getExtraResponseHeaders());
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                    , HttpResponseStatus.valueOf(response.getFutureResponse().getStatusCode()),
                    content);
            httpResponse.headers().add(response.getFutureResponse().getHeaders());
            return httpResponse;
        }
    }
}
