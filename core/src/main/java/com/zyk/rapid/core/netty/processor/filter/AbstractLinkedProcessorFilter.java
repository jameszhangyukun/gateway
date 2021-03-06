package com.zyk.rapid.core.netty.processor.filter;

import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.helper.ResponseHelper;

/**
 * 抽象带有链表的过滤器
 */
public abstract class AbstractLinkedProcessorFilter<T> implements ProcessorFilter<Context> {
    /**
     * 链表的元素 持有下一个元素的引用
     */
    protected AbstractLinkedProcessorFilter<T> next;

    /**
     * 执行下一个元素
     *
     * @param context
     * @param args
     * @throws Throwable
     */
    @Override
    public void fireNext(Context context, Object... args) throws Throwable {
        // 上下文生命周期的作用
        if (context.isTerminated()) {
            return;
        }
        if (context.isWrittened()) {
            ResponseHelper.writeResponse(context);
        }
        if (next != null) {
            if (!next.check(context)) {
                next.fireNext(context, args);
            } else {
                next.transformEntry(context, args);
            }
        } else {
            context.terminated();
        }
    }

    /**
     * 子类调用 真正执行下一个节点的操作
     *
     * @param context
     * @param args
     * @throws Throwable
     */
    @Override
    public void transformEntry(Context context, Object... args) throws Throwable {
        entry(context, args);
    }

    public AbstractLinkedProcessorFilter<T> getNext() {
        return next;
    }

    public void setNext(AbstractLinkedProcessorFilter<T> next) {
        this.next = next;
    }


}
