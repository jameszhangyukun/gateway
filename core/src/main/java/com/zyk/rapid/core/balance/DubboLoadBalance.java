package com.zyk.rapid.core.balance;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.enums.LoadBalanceStrategy;
import com.zyk.gateway.common.enums.ResponseCode;
import com.zyk.gateway.common.exception.RapidResponseException;
import com.zyk.rapid.core.context.AttributeKey;
import com.zyk.rapid.core.context.RapidContext;
import com.zyk.rapid.core.helper.DubboReferenceHelper;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_WEIGHT;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;

/**
 * 使用Dubbo SPI扩展点实现
 */
public class DubboLoadBalance implements LoadBalance {
    public static final String NAME = "rlb";

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        System.out.println("----------------dubbo into-----------------------");
        RapidContext rapidContext = (RapidContext) RpcContext.getContext().get(DubboReferenceHelper.DUBBO_TRANSFER_CONTEXT);
        LoadBalanceStrategy balanceStrategy = rapidContext.getAttribute(AttributeKey.DUBBO_LOAD_BALANCE_STRATEGY);
        com.zyk.rapid.core.balance.LoadBalance loadBalance = LoadBalanceFactory.getLoadBalance(balanceStrategy);
        Set<ServiceInstance> serviceInstanceWrappers = new HashSet<>();
        for (Invoker<?> invoker : invokers) {
            serviceInstanceWrappers.add(new ServiceInvokerWrapper<>(invoker, invocation));
        }
        // 把dubbo invoker转换为ServiceInstance
        rapidContext.putAttribute(AttributeKey.MATCH_INSTANCE, serviceInstanceWrappers);
        ServiceInstance instance = loadBalance.select(rapidContext);
        if (instance instanceof ServiceInvokerWrapper) {
            return ((ServiceInvokerWrapper) instance).getInvoker();
        } else {
            throw new RapidResponseException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }

    }

    public static class ServiceInvokerWrapper<T> extends ServiceInstance {

        private static final long serialVersionUID = -208654348467793074L;

        private final Invoker<T> invoker;

        public ServiceInvokerWrapper(Invoker<T> invoker, Invocation invocation) {
            this.invoker = invoker;
            this.setServiceInstanceId(invoker.getUrl().getAddress());
            this.setUniqueId(invoker.getUrl().getServiceKey());
            this.setRegisterTime(invoker.getUrl().getParameter(CommonConstants.TIMESTAMP_KEY, 0L));
            this.setEnable(true);
            this.setVersion(invoker.getUrl().getParameter(CommonConstants.VERSION_KEY));
            this.setWeight(invoker.getUrl().getMethodParameter(invocation.getMethodName(), WEIGHT_KEY, DEFAULT_WEIGHT));
            this.setAddress(invoker.getUrl().getAddress());
        }

        public Invoker<T> getInvoker() {
            return invoker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceInvokerWrapper<?> that = (ServiceInvokerWrapper<?>) o;
            return Objects.equals(this.getAddress(), that.getAddress());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getAddress());
        }
    }
}
