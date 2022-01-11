package com.zyk.rapid.core.netty.processor.filter;

import com.zyk.rapid.core.context.Context;

public class DefaultProcessorFilterChain extends ProcessorFilterChain<Context> {
    private final String id;

    public DefaultProcessorFilterChain(String id) {
        this.id = id;
    }

    AbstractLinkedProcessorFilter<Context> first = new AbstractLinkedProcessorFilter<Context>() {
        @Override
        public boolean check(Context context) throws Throwable {
            return true;
        }

        @Override
        public void entry(Context context, Object... args) throws Throwable {
            super.fireNext(context, args);
        }
    };

    AbstractLinkedProcessorFilter<Context> end = first;

    public String getId() {
        return id;
    }


    @Override
    public boolean check(Context context) throws Throwable {
        return true;
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        first.transformEntry(context, args);
    }

    @Override
    public void addFirst(AbstractLinkedProcessorFilter<Context> filter) {
        filter.setNext(first.next);
        first.setNext(filter);
        if (end == first) {
            end = filter;
        }
    }

    @Override
    public void addLast(AbstractLinkedProcessorFilter<Context> filter) {
        end.setNext(filter);
        end = filter;
    }

    @Override
    public void setNext(AbstractLinkedProcessorFilter<Context> next) {
        addLast(next);
    }

    @Override
    public AbstractLinkedProcessorFilter<Context> getNext() {
        return first.getNext();
    }
}
