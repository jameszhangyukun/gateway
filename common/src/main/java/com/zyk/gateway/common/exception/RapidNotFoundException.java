package com.zyk.gateway.common.exception;

import com.zyk.gateway.common.enums.ResponseCode;

/**
 * 服务信息未找到异常
 */
public class RapidNotFoundException extends RapidBaseException {
    private static final long serialVersionUID = -7082148386647670905L;

    public RapidNotFoundException(ResponseCode responseCode) {
        super(responseCode.getMessage(), responseCode);
    }

    public RapidNotFoundException(Throwable cause, ResponseCode responseCode) {
        super(responseCode.getMessage(), cause, responseCode);
    }
}
