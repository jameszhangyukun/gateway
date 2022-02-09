package com.zyk.rapid.core.netty.processor.filter.pre;

import com.zyk.gateway.common.config.DynamicConfigManager;
import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.gateway.common.enums.LoadBalanceStrategy;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.rapid.core.balance.LoadBalance;
import com.zyk.rapid.core.balance.LoadBalanceFactory;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.context.RapidRequest;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

import static com.zyk.gateway.common.constants.RapidProtocol.DUBBO;
import static com.zyk.gateway.common.constants.RapidProtocol.HTTP;

/**
 * 负载均衡前置过滤器
 */
@Filter(id = ProcessFilterConstants.LOAD_BALANCE_PRE_FILTER_ID,
        value = ProcessorFilterType.PRE,
        order = ProcessFilterConstants.LOAD_BALANCE_PRE_FILTER_ORDER,
        name = ProcessFilterConstants.LOAD_BALANCE_PRE_FILTER_NAME
)
public class LoadBalancePreFilter extends AbstractEntryProcessorFilter<LoadBalancePreFilter.Config> {

    public LoadBalancePreFilter() {
        super(LoadBalancePreFilter.Config.class);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        try {
            RapidContext rapidContext = (RapidContext) context;
            LoadBalancePreFilter.Config config = (LoadBalancePreFilter.Config) args[0];
            LoadBalanceStrategy loadBalanceStrategy = config.getLoadBalanceStrategy();
            String protocol = rapidContext.getProtocol();
            switch (protocol) {
                case HTTP:
                    doHttpLoadBalance(rapidContext, loadBalanceStrategy);
                    break;
                case DUBBO:
                    doDubboLoadBalance(rapidContext, loadBalanceStrategy);
                    break;
                default:
                    break;

            }
        } finally {
            super.fireNext(context, args);
        }
    }

    private void doHttpLoadBalance(RapidContext rapidContext, LoadBalanceStrategy loadBalanceStrategy) {
        RapidRequest rapidRequest = rapidContext.getOriginRequest();
        String uniqueId = rapidRequest.getUniqueId();
        Set<ServiceInstance> serviceInstances = DynamicConfigManager.getInstance().getServiceInstances(uniqueId);
        rapidContext.putAttribute(AttributeKey.MATCH_INSTANCE, serviceInstances);

        // 获取负载均衡策略对象
        LoadBalance loadBalance = LoadBalanceFactory.getLoadBalance(loadBalanceStrategy);
        ServiceInstance serviceInstance = loadBalance.select(rapidContext);
        if (serviceInstance == null) {
            // 如果服务实例未找到
            // 终止请求执行，显示异常
            rapidContext.terminated();
            throw new RapidResponseException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        rapidContext.getRequestMutable().setModifyHost(serviceInstance.getAddress());
    }

    private void doDubboLoadBalance(RapidContext rapidContext, LoadBalanceStrategy loadBalanceStrategy) {
        // 将负载均衡策略设置到上下文即可，由dubbo LoadBalance去使用
        rapidContext.putAttribute(AttributeKey.DUBBO_LOAD_BALANCE_STRATEGY, loadBalanceStrategy);
    }

    /**
     * 配置
     */
    @Getter
    @Setter
    public static class Config extends FilterConfig {
        private LoadBalanceStrategy loadBalanceStrategy = LoadBalanceStrategy.RANDOM;
    }

}
