package com.zyk.rapid.core.netty.processor.filter.error;

import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidBaseException;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.context.RapidResponse;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;

/**
 * 默认错误过滤器
 */

@Filter(id = ProcessFilterConstants.DEFAULT_ERROR_FILTER_ID,
        value = ProcessorFilterType.ERROR,
        order = ProcessFilterConstants.DEFAULT_ERROR_FILTER_FILTER_ORDER,
        name = ProcessFilterConstants.DEFAULT_ERROR_FILTER_NAME
)
public class DefaultErrorFilter extends AbstractEntryProcessorFilter<FilterConfig> {


    public DefaultErrorFilter() {
        super(FilterConfig.class);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        try {
            Throwable throwable = context.getThrowable();
            ResponseCode responseCode = ResponseCode.INTERNAL_ERROR;
            if (throwable instanceof RapidBaseException) {
                RapidBaseException rapidBaseException = (RapidBaseException) throwable;
                responseCode = rapidBaseException.getCode();
            }
            RapidResponse rapidResponse = RapidResponse.buildRapidResponse(responseCode);
            context.setResponse(rapidResponse);
        } finally {
            System.out.println("============= do error filter ===============");
            context.writtened();
            super.fireNext(context, args);
        }
    }
}
