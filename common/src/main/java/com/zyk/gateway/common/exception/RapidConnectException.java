package com.zyk.gateway.common.exception;

import com.zyk.gateway.common.enums.ResponseCode;
import lombok.Getter;

/**
 * 连接异常定义类
 */
public class RapidConnectException extends RapidBaseException {
    private static final long serialVersionUID = -1988974087233877512L;

    @Getter
    private final String uniqueId;
    @Getter
    private final String requestUrl;

    public RapidConnectException(Throwable throwable, String uniqueId, String url, ResponseCode code) {
        super(code.getMessage(), throwable, code);
        this.uniqueId = uniqueId;
        this.requestUrl = url;
    }

    public RapidConnectException(String uniqueId, String url) {
        this.uniqueId = uniqueId;
        this.requestUrl = url;
    }

}
