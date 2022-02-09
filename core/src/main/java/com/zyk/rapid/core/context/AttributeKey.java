package com.zyk.rapid.core.context;

import com.zyk.gateway.common.config.ServiceInstance;
import com.zyk.gateway.common.config.ServiceInvoker;
import com.zyk.gateway.common.enums.LoadBalanceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 属性上下文的抽象类
 *
 * @param <T>
 */
public abstract class AttributeKey<T> {

    public static final AttributeKey<LoadBalanceStrategy> DUBBO_LOAD_BALANCE_STRATEGY = crete(LoadBalanceStrategy.class);
    private static final Map<String, AttributeKey<?>> namedMap = new HashMap<>();

    public static final AttributeKey<ServiceInstance> LOAD_SERVICE_INSTANCE = crete(ServiceInstance.class);

    /**
     * 存储所有实例的信息，负载均衡使用
     */
    public static final AttributeKey<Set<ServiceInstance>> MATCH_INSTANCE = crete(Set.class);

    public static final AttributeKey<Set<String>> MATCH_ADDRESS = crete(Set.class);

    public static final AttributeKey<ServiceInvoker> HTTP_INVOKER = crete(ServiceInvoker.class);

    public static final AttributeKey<ServiceInvoker> DUBBO_INVOKER = crete(ServiceInvoker.class);
    //  Dubbo请求附加参数透传
    public static final AttributeKey<Map<String, String>> DUBBO_ATTACHMENT = crete(Map.class);
    static {
        namedMap.put("MATCH_ADDRESS", MATCH_ADDRESS);
        namedMap.put("HTTP_INVOKER", HTTP_INVOKER);
        namedMap.put("DUBBO_INVOKER", DUBBO_INVOKER);
        namedMap.put("MATCH_INSTANCE", MATCH_INSTANCE);
        namedMap.put("DUBBO_LOAD_BALANCE",DUBBO_LOAD_BALANCE_STRATEGY);
        namedMap.put("DUBBO_ATTACHMENT",DUBBO_ATTACHMENT);

    }

    public abstract T cast(Object value);

    public static <T> AttributeKey<T> crete(final Class<? super T> valueClass) {
        return new SimpleAttributeKey(valueClass);
    }

    public static class SimpleAttributeKey<T> extends AttributeKey<T> {
        private final Class<T> valueClass;

        public SimpleAttributeKey(Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        @Override
        public T cast(Object value) {
            return valueClass.cast(value);
        }

        @Override
        public String toString() {
            if (valueClass != null) {
                String sb = getClass().getName() + "<" +
                        valueClass.getName() +
                        ">";
                return sb;
            }
            return super.toString();
        }
    }
}
