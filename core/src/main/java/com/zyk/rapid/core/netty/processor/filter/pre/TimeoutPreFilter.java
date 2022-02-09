package com.zyk.rapid.core.netty.processor.filter.pre;

import com.zyk.gateway.common.config.DubboServiceInvoker;
import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.context.RapidRequest;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.zyk.gateway.common.constants.RapidProtocol.DUBBO;
import static com.zyk.gateway.common.constants.RapidProtocol.HTTP;

@Filter(id = ProcessFilterConstants.TIME_OUT_PRE_FILTER_ID,
        value = ProcessorFilterType.PRE,
        order = ProcessFilterConstants.TIME_OUT_PRE_FILTER_ORDER,
        name = ProcessFilterConstants.TIME_OUT_PRE_FILTER_NAME
)
public class TimeoutPreFilter extends AbstractEntryProcessorFilter<TimeoutPreFilter.Config> {

    public TimeoutPreFilter() {
        super(TimeoutPreFilter.Config.class);
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Config extends FilterConfig {
        private Integer timeout;
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        try {
            RapidContext rapidContext = (RapidContext) context;
            String protocol = rapidContext.getProtocol();
            TimeoutPreFilter.Config config = (TimeoutPreFilter.Config) args[0];
            switch (protocol) {
                case HTTP:
                    RapidRequest request = rapidContext.getRequest();
                    request.setRequestTimeout(config.timeout);
                    break;
                case DUBBO:
                    DubboServiceInvoker dubboServiceInvoker = (DubboServiceInvoker) rapidContext.getRequiredAttribute(AttributeKey.DUBBO_INVOKER);
                    dubboServiceInvoker.setTimeout(config.timeout);
                    break;
                default:
                    break;
            }
        } finally {
            super.fireNext(context, args);
        }

    }
}
