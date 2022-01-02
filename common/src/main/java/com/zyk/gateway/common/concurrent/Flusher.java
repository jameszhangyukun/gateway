package com.zyk.gateway.common.concurrent;

/**
 * Flusher接口定义
 */
public interface Flusher<E> {
    /**
     * 添加元素
     *
     * @param event
     */
    void add(E event);

    /**
     * 添加多个元素
     *
     * @param events
     */
    void add(E... events);

    boolean tryAdd(E event);

    boolean tryAdd(E... events);

    boolean isShutdown();

    void start();

    void shutdown();
}

