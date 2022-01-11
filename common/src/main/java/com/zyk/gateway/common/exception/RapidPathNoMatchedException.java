package com.zyk.gateway.common.exception;

import com.zyk.gateway.common.enums.ResponseCode;

public class RapidPathNoMatchedException extends RapidBaseException {
    private static final long serialVersionUID = 2393718607482771588L;

    public RapidPathNoMatchedException() {
        this(ResponseCode.PATH_NO_MATCHED);
    }

    public RapidPathNoMatchedException(ResponseCode responseCode) {
        super(responseCode.getMessage(), responseCode);
    }

    public RapidPathNoMatchedException(Throwable cause, ResponseCode responseCode) {
        super(responseCode.getMessage(), cause, responseCode);
    }
}
