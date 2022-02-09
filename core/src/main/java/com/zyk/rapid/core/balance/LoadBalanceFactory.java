package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.enums.LoadBalanceStrategy;

import java.util.HashMap;
import java.util.Map;

public class LoadBalanceFactory {

    private final Map<LoadBalanceStrategy, LoadBalance> loadBalanceMap = new HashMap<>();

    private static final LoadBalanceFactory INSTANCE = new LoadBalanceFactory();

    private LoadBalanceFactory() {
        loadBalanceMap.put(LoadBalanceStrategy.RANDOM, new RandomLoadBalance());
        loadBalanceMap.put(LoadBalanceStrategy.ROUND_ROBIN, new RoundRobinLoadBalance());

    }

    public static LoadBalance getLoadBalance(LoadBalanceStrategy loadBalanceStrategy) {
        return INSTANCE.loadBalanceMap.get(loadBalanceStrategy);
    }
}

