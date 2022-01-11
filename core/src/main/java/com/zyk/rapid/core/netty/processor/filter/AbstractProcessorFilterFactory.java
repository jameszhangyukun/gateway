package com.zyk.rapid.core.netty.processor.filter;

import com.zyk.rapid.core.context.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽象的过滤器工厂
 */
@Slf4j
public abstract class AbstractProcessorFilterFactory implements ProcessorFilterFactory {

    protected DefaultProcessorFilterChain defaultProcessorFilterChain = new DefaultProcessorFilterChain("defaultProcessorFilterChain");

    protected DefaultProcessorFilterChain errorProcessorFilterChain = new DefaultProcessorFilterChain("errorProcessorFilterChain");

    private Map<String, Map<String, ProcessorFilter<Context>>> processorFilterTypeMap = new LinkedHashMap<>();

    private Map<String, ProcessorFilter<Context>> processorFilterIdMap = new LinkedHashMap<>();

    /**
     * 构建过滤器链条
     *
     * @param filterType
     * @param filters
     * @throws Exception
     */
    @Override
    public void buildFilterChain(ProcessorFilterType filterType, List<ProcessorFilter<Context>> filters) throws Exception {
        switch (filterType) {
            case PRE:
            case ROUTE:
                addFilterForChain(defaultProcessorFilterChain, filters);
                break;
            case ERROR:
                addFilterForChain(errorProcessorFilterChain, filters);
                break;
            case POST:
                addFilterForChain(defaultProcessorFilterChain, filters);
                addFilterForChain(errorProcessorFilterChain, filters);
                break;
            default:
                throw new RuntimeException("FilterType " + filterType.getCode() + "do not supported");
        }
    }

    private void addFilterForChain(DefaultProcessorFilterChain processorFilterChain, List<ProcessorFilter<Context>> filters) throws Exception {
        for (ProcessorFilter<Context> filter : filters) {
            filter.init();
            doBuilder(processorFilterChain, filter);
        }
    }

    private void doBuilder(DefaultProcessorFilterChain processorFilterChain, ProcessorFilter<Context> filter) {
        log.info("filterChain: {} the scanner filter is : {}", processorFilterChain.getId(), filter.getClass().getName());
        Filter annotation = filter.getClass().getAnnotation(Filter.class);
        if (annotation != null) {
            processorFilterChain.addLast((AbstractLinkedProcessorFilter<Context>) filter);
            // 映射到过滤器集合
            String filterId = annotation.id();
            if (filterId == null || filterId.length() < 1) {
                filterId = annotation.name();
            }
            String code = annotation.value().getCode();
            Map<String, ProcessorFilter<Context>> processorFilterMap = processorFilterTypeMap.get(code);
            if (processorFilterMap == null) {
                processorFilterMap = new LinkedHashMap<>();
            }
            processorFilterMap.put(filterId, filter);

            processorFilterTypeMap.put(code, processorFilterMap);
            processorFilterIdMap.put(filterId, filter);
        }
    }

    @Override
    public <T> T getFilter(Class<T> t) throws Exception {
        Filter annotation = t.getAnnotation(Filter.class);
        if (annotation != null) {
            String filterId = annotation.id();
            if (filterId == null || filterId.length() < 1) {
                filterId = annotation.name();
            }
            return getFilter(filterId);
        }
        return null;
    }

    @Override
    public <T> T getFilter(String filterId) throws Exception {
        ProcessorFilter<Context> processorFilter = null;
        if (!processorFilterIdMap.isEmpty()) {
            processorFilter = processorFilterIdMap.get(filterId);
        }
        return (T) processorFilter;
    }
}
