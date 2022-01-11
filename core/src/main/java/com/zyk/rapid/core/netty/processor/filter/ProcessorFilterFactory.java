package com.zyk.rapid.core.netty.processor.filter;

import com.zyk.rapid.core.context.Context;

import java.util.List;

public interface ProcessorFilterFactory {

    void buildFilterChain(ProcessorFilterType filterType, List<ProcessorFilter<Context>> filters) throws Exception;

    void doFilterChain(Context context) throws Exception;

    void doErrorFilterChain(Context context) throws Exception;

    <T> T getFilter(Class<T> t) throws Exception;

    <T> T getFilter(String filterId) throws Exception;
}
