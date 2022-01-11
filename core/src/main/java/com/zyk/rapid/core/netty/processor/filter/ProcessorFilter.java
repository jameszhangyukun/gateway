package com.zyk.rapid.core.netty.processor.filter;

/**
 * 过滤器接口
 *
 * @param <T>
 */
public interface ProcessorFilter<T> {
    /**
     * 过滤器是否执行的校验方法
     *
     * @param t
     * @return
     * @throws Exception
     */
    boolean check(T t) throws Throwable;

    /**
     * 真正执行过滤器的方法
     *
     * @param t
     * @param args
     * @throws Exception
     */
    void entry(T t, Object... args) throws Throwable;

    /**
     * 触发下一个过滤器执行
     *
     * @param t
     * @param args
     * @throws Throwable
     */
    void fireNext(T t, Object... args) throws Throwable;

    void transformEntry(T t, Object... args) throws Throwable;

    /**
     * 过滤器的初始化方法
     *
     * @throws Exception
     */
    default void init() throws Exception {

    }

    /**
     * 销毁方法
     *
     * @throws Exception
     */
    default void destroy() throws Exception {

    }

    /**
     * 刷新方法
     *
     * @throws Exception
     */
    default void refresh() throws Exception {

    }

}
