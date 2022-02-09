package com.zyk.rapid.core.netty.processor.filter.post;

import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import lombok.Getter;
import lombok.Setter;

/**
 * 后置过滤器
 */
@Filter(id = ProcessFilterConstants.STATISTIC_POST_FILTER_ID,
        value = ProcessorFilterType.POST,
        order = ProcessFilterConstants.STATISTIC_POST_FILTER_ORDER,
        name = ProcessFilterConstants.STATISTIC_POST_FILTER_NAME
)
public class StatisticPostFilter extends AbstractEntryProcessorFilter<StatisticPostFilter.Config> {
    public StatisticPostFilter() {
        super(StatisticPostFilter.Config.class);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        try {

        } finally {
            // 如果是最后一个filter
            context.terminated();
            super.fireNext(context, args);
        }
    }

    @Getter
    @Setter
    public static class Config extends FilterConfig {
        private boolean rollingNumber = true;
    }
}
