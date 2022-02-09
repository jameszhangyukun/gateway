package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.rapid.core.context.RapidContext;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinLoadBalance extends AbstractLoadBalance {
    private ConcurrentHashMap<String, ConcurrentHashMap<String, WeightedRoundRobin>> pathWeightMap
            = new ConcurrentHashMap<>();

    private static final int RECYCLE_PERIOD = 60000;

    @Override
    protected ServiceInstance doSelect(RapidContext rapidContext, List<ServiceInstance> serviceInstances) {
        String path = rapidContext.getOriginRequest().getPath();
        ConcurrentHashMap<String, WeightedRoundRobin> map = pathWeightMap.computeIfAbsent(path, key -> new ConcurrentHashMap<>());

        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        ServiceInstance selectedInstance = null;
        WeightedRoundRobin selectedWRR = null;
        for (ServiceInstance instance : serviceInstances) {
            String address = instance.getAddress();
            int weight = getWeight(instance);
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(address, key -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });
            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);
            }
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedWRR = weightedRoundRobin;
                selectedInstance = instance;
            }
            totalWeight += weight;
        }
        if (serviceInstances.size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }
        if (selectedInstance != null) {
            selectedWRR.sel(totalWeight);
            return selectedInstance;
        }
        return serviceInstances.get(0);
    }

    /**
     * 缓存权重
     */
    protected static class WeightedRoundRobin {
        // 实例权重
        private int weight;
        // 当前权重
        private AtomicLong current = new AtomicLong(0);
        // 最后一次更新事件
        private long lastUpdate;

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }

        public long increaseCurrent() {
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
}
