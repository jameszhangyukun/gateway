package com.zyk.rapid.discovery.api;

/**
 * 注册接口
 */
public interface Notify {
    /**
     * 添加或更新
     *
     * @param key
     * @param value
     * @throws Exception
     */
    void put(String key, String value) throws Exception;

    /**
     * 删除key
     *
     * @param key
     * @throws Exception
     */
    void delete(String key) throws Exception;
}
