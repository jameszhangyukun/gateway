package com.zyk.rapid.core.helper;

import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.rapid.core.context.RapidResponse;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class ResponseHelper {
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode) {
        RapidResponse rapidResponse = RapidResponse.buildRapidResponse(responseCode);
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1
                , HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(rapidResponse.getContent().getBytes(StandardCharsets.UTF_8)));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }
}
