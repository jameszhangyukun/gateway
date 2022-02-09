package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.rapid.core.context.RapidContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    /**
     * 随机负载均衡方法
     *
     * @param rapidContext
     * @param serviceInstances
     * @return
     */
    @Override
    protected ServiceInstance doSelect(RapidContext rapidContext, List<ServiceInstance> serviceInstances) {
        int length = serviceInstances.size();
        int totalWeight = 0;
        boolean sameWeight = true;

        for (int i = 0; i < length; i++) {
            int weight = getWeight(serviceInstances.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0 && weight != getWeight(serviceInstances.get(i - 1))) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (ServiceInstance instance : serviceInstances) {
                offset = offset - getWeight(instance);
                if (offset < 0) return instance;
            }
        }
        return serviceInstances.get(ThreadLocalRandom.current().nextInt(length));
    }

}
