package com.zyk.rapid.core.helper;

import com.zyk.gateway.common.enums.ResponseCode;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class ResponseHelper {
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode) {
        // TODO 目前还没有Response对象
        String errorContent = "响应错误";
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                , HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(errorContent.getBytes(StandardCharsets.UTF_8)));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorContent.length());
        return httpResponse;
    }
}
