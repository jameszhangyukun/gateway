package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.RapidContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 抽象负载均衡：主要实现预热
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public ServiceInstance select(RapidContext rapidContext) {
        Set<ServiceInstance> matchInstance = rapidContext.getRequiredAttribute(AttributeKey.MATCH_INSTANCE);
        if (matchInstance == null || matchInstance.size() == 0) {
            return null;
        }
        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>(matchInstance);
        if (matchInstance.size() == 1) {
            return serviceInstances.get(0);
        }
        ServiceInstance serviceInstance = doSelect(rapidContext, serviceInstances);
        rapidContext.putAttribute(AttributeKey.LOAD_SERVICE_INSTANCE, serviceInstance);
        return serviceInstance;
    }

    /**
     * 子类实现轮序策略选择服务
     *
     * @param rapidContext
     * @param serviceInstances
     * @return
     */
    protected abstract ServiceInstance doSelect(RapidContext rapidContext, List<ServiceInstance> serviceInstances);

    protected static int getWeight(ServiceInstance serviceInstance) {
        int weight = serviceInstance.getWeight() == null ? LoadBalance.DEFAULT_WEIGHT : serviceInstance.getWeight();
        if (weight > 0) {
            long registerTime = serviceInstance.getRegisterTime();
            if (registerTime > 0) {
                // 服务启动时间多久：当前事件-注册事件
                int upTime = (int) (TimeUtil.currentTimeMillis() - registerTime);
                // 默认预热事件
                int warmup = LoadBalance.DEFAULT_WARMUP;
                if (upTime > 0 && upTime < warmup) {
                    weight = calculateWarmUpWeight(upTime, warmup, weight);
                }
            }
        }
        return weight;
    }

    /**
     * 计算服务在预热事件内的新权重
     *
     * @param upTime
     * @param warmup
     * @param weight
     * @return
     */
    private static int calculateWarmUpWeight(int upTime, int warmup, int weight) {
        int ww = (int) ((float) upTime / (float) warmup / (float) (weight));

        return ww < 1 ? 1 : (Math.min(ww, weight));
    }
}
