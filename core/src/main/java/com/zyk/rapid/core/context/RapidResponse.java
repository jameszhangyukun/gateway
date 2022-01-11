package com.zyk.rapid.core.context;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.util.JSONUtil;
import io.netty.handler.codec.http.*;
import lombok.Data;
import org.asynchttpclient.Response;

/**
 * 网关响应封装类
 */
@Data
public class RapidResponse {
    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    private final HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();
    /**
     * 响应内容
     */
    private String content;

    private HttpResponseStatus httpResponseStatus;

    // 响应对象
    private Response futureResponse;

    private RapidResponse() {

    }

    public void putHeader(CharSequence key, CharSequence value) {
        responseHeaders.add(key, value);
    }

    /**
     * 构建响应对象
     *
     * @param response
     * @return
     */
    public static RapidResponse buildRapidResponse(Response response) {
        RapidResponse rapidResponse = new RapidResponse();
        rapidResponse.setFutureResponse(response);
        rapidResponse.setHttpResponseStatus(HttpResponseStatus.valueOf(response.getStatusCode()));
        return rapidResponse;
    }

    /**
     * 返回JSON类型的响应信息
     */
    public static RapidResponse buildRapidResponse(ResponseCode code, Object... args) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        objectNode.put(JSONUtil.CODE, code.getCode());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());
        RapidResponse rapidResponse = new RapidResponse();
        rapidResponse.setHttpResponseStatus(code.getStatus());
        rapidResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        rapidResponse.setContent(JSONUtil.toJSONString(objectNode));
        return rapidResponse;
    }

    public static RapidResponse buildRapidResponseObj(Object data) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        objectNode.putPOJO(JSONUtil.DATA, data);
        RapidResponse rapidResponse = new RapidResponse();
        rapidResponse.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        rapidResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        rapidResponse.setContent(JSONUtil.toJSONString(objectNode));
        return rapidResponse;
    }
}
