package com.zyk.rapid.core.netty.processor.filter;

import com.zyk.gateway.common.util.ServiceLoader;
import com.zyk.rapid.core.context.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class DefaultProcessorFilterFactory extends AbstractProcessorFilterFactory {
    /**
     * 构造方法 加载所有的ProcessorFilter子类的实现
     */
    private DefaultProcessorFilterFactory() {
        Map<String, List<ProcessorFilter<Context>>> processorFilterMap = new LinkedHashMap<>();
        // 1.通过ServiceLoader加载
        ServiceLoader<ProcessorFilter> serviceLoader = ServiceLoader.load(ProcessorFilter.class);
        for (ProcessorFilter<Context> processorFilter : serviceLoader) {
            Filter annotation = processorFilter.getClass().getAnnotation(Filter.class);
            if (annotation != null) {
                String type = annotation.value().getCode();
                List<ProcessorFilter<Context>> filterList = processorFilterMap.get(type);
                if (filterList == null) {
                    filterList = new ArrayList<>();
                }
                filterList.add(processorFilter);
                processorFilterMap.put(type, filterList);
            }
        }
        for (ProcessorFilterType processorFilterType : ProcessorFilterType.values()) {
            List<ProcessorFilter<Context>> processorFilters = processorFilterMap.get(processorFilterType.getCode());
            if (processorFilters == null || processorFilters.isEmpty()) {
                continue;
            }
            Collections.sort(processorFilters, new Comparator<ProcessorFilter<Context>>() {
                @Override
                public int compare(ProcessorFilter<Context> o1, ProcessorFilter<Context> o2) {
                    return o1.getClass().getAnnotation(Filter.class).order() - o2.getClass().getAnnotation(Filter.class).order();
                }
            });
            try {
                super.buildFilterChain(processorFilterType, processorFilters);
            } catch (Exception e) {
                log.error("#DefaultProcessorFilterFactory.buildFilterChain failed");
            }
        }
    }

    @Override
    public void doFilterChain(Context context) throws Exception {
        try {
            defaultProcessorFilterChain.entry(context);
        } catch (Throwable e) {
            log.error("#DefaultProcessorFilterFactory.doFilterChain # ERROR MESSAGE{}", e.getMessage(), e);
            // 设置异常
            context.setThrowable(e);
            // 执行doFilterChain显式抛出异常时 Context的生命周期为TERNIMATED
            if (context.isTerminated()) {
                context.runned();
            }
            // 执行异常处理器链条
            doErrorFilterChain(context);
        }
    }

    @Override
    public void doErrorFilterChain(Context context) throws Exception {
        try {
            errorProcessorFilterChain.entry(context);
        } catch (Throwable e) {
            log.error("#DefaultProcessorFilterFactory.doErrorFilterChain # ERROR MESSAGE{}", e.getMessage(), e);
        }
    }

    private static class SingletonHolder {
        private static final DefaultProcessorFilterFactory INSTANCE = new DefaultProcessorFilterFactory();
    }

    public static DefaultProcessorFilterFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
