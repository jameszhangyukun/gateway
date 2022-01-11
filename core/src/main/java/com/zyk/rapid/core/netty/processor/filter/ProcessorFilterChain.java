package com.zyk.rapid.core.netty.processor.filter;


public abstract class ProcessorFilterChain<T> extends AbstractLinkedProcessorFilter<T> {
    /**
     * 在链表头部添加元素
     *
     * @param filter
     */
    public abstract void addFirst(AbstractLinkedProcessorFilter<T> filter);

    /**
     * 在链表尾部添加元素
     *
     * @param filter
     */
    public abstract void addLast(AbstractLinkedProcessorFilter<T> filter);
}

