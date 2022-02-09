package com.zyk.gateway.common.constants;

/**
 * 所有过滤器常量配置定义
 */
public interface ProcessFilterConstants {
    String TIME_OUT_PRE_FILTER_ID = "timeoutPreFilter";
    String TIME_OUT_PRE_FILTER_NAME = "超时过滤器";
    int TIME_OUT_PRE_FILTER_ORDER = 2100;

    String LOAD_BALANCE_PRE_FILTER_ID = "loadBalancePreFilter";
    String LOAD_BALANCE_PRE_FILTER_NAME = "负载均衡过滤器";
    int LOAD_BALANCE_PRE_FILTER_ORDER = 2000;

    String HTTP_ROUTER_FILTER_ID = "httpRouterFilter";
    String HTTP_ROUTER_FILTER_NAME = "HTTP请求中置过滤器";
    int HTTP_ROUTER_FILTER_ORDER = 5000;

    String DEFAULT_ERROR_FILTER_ID = "defaultErrorFilter";
    String DEFAULT_ERROR_FILTER_NAME = "默认错误处理器";
    int DEFAULT_ERROR_FILTER_FILTER_ORDER = 20000;

    String STATISTIC_POST_FILTER_ID = "statisticPostFilter";
    String STATISTIC_POST_FILTER_NAME = "最后的统计分析过滤器";
    int STATISTIC_POST_FILTER_ORDER = Integer.MAX_VALUE;
    String DUBBO_ROUTER_FILTER_ID = "dubboRouteFilter";
    int DUBBO_ROUTER_FILTER_ORDER = 20000;
    String DUBBO_ROUTER_FILTER_NAME = "DUBBO请求中置过滤器";
}
