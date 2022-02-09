package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.rapid.core.context.RapidContext;

/**
 * 负载均衡最上层接口定义
 */
public interface LoadBalance {
    int DEFAULT_WEIGHT = 100;
    /**
     * 预热
     */
    int DEFAULT_WARMUP = 5 * 60 * 1000;

    /**
     * 从所有实例列表中中选择服务发送请求
     * @param rapidContext
     * @return
     */
    ServiceInstance select(RapidContext rapidContext);
}
