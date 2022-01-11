package com.zyk.rapid.core.context;

import com.zyk.gateway.common.config.ServiceInvoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 属性上下文的抽象类
 *
 * @param <T>
 */
public abstract class AttributeKey<T> {

    private static final Map<String, AttributeKey<?>> namedMap = new HashMap<>();

    public static final AttributeKey<Set<String>> MATCH_ADDRESS = crete(Set.class);

    public static final AttributeKey<ServiceInvoker> HTTP_INVOKER = crete(ServiceInvoker.class);

    public static final AttributeKey<ServiceInvoker> DUBBO_INVOKER = crete(ServiceInvoker.class);

    static {
        namedMap.put("MATCH_ADDRESS", MATCH_ADDRESS);
        namedMap.put("HTTP_INVOKER", HTTP_INVOKER);
        namedMap.put("DUBBO_INVOKER", DUBBO_INVOKER);
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
