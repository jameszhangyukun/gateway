package com.zyk.rapid.core.netty.processor.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.zyk.gateway.common.config.Rule;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.util.JSONUtil;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.netty.processor.cache.DefaultCacheManager;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractEntryProcessorFilter<FilterConfigClass> extends AbstractLinkedProcessorFilter<Context> {

    private Filter filterAnnotation;

    protected Cache<String, FilterConfigClass> cache;

    private final Class<FilterConfigClass> filterConfigClass;

    public AbstractEntryProcessorFilter(Class<FilterConfigClass> filterConfigClass) {
        this.filterAnnotation = this.getClass().getAnnotation(Filter.class);
        this.filterConfigClass = filterConfigClass;
        this.cache = DefaultCacheManager.getInstance().create(DefaultCacheManager.FILTER_CONFIG_CACHE_ID);
    }

    @Override
    public boolean check(Context context) throws Throwable {
        return context.getRule().hasId(filterAnnotation.id());
    }

    @Override
    public void transformEntry(Context context, Object... args) throws Throwable {
        System.out.println("parent");
        FilterConfigClass filterConfigClass = dynamicLoadCache(context, args);
        super.transformEntry(context, filterConfigClass);
    }

    private FilterConfigClass dynamicLoadCache(Context context, Object[] args) {
        Rule.FilterConfig filterConfig = context.getRule().getFilterConfig(filterAnnotation.id());
        String ruleId = context.getRule().getId();
        String cacheKey = ruleId + BasicConst.DOLLAR_SEPARATOR + filterAnnotation.id();
        FilterConfigClass filterConfigClass = cache.getIfPresent(cacheKey);
        if (filterConfigClass == null) {
            if (filterConfig != null && StringUtils.isNoneBlank(filterConfig.getConfig())) {
                String configStr = filterConfig.getConfig();
                filterConfigClass = JSONUtil.parse(configStr, this.filterConfigClass);
                cache.put(cacheKey, filterConfigClass);
            }
        }
        return filterConfigClass;
    }
}
