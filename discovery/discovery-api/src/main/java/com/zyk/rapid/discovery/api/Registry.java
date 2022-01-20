package com.zyk.rapid.discovery.api;

import com.zyk.gateway.common.util.Pair;

import java.util.List;

/**
 * 注册接口
 */
public interface Registry {
    /**
     * 存放所有服务定义信息
     */
    String SERVICE_PREFIX = "/services";
    /**
     * 存储所有服务实例的信息
     */
    String INSTANCE_PREFIX = "/instances";
    /**
     * 存储接口规则信息
     */
    String RULE_PREFIX = "/rules";
    /**
     * 存储所有网关本身自注册信息
     */
    String GATEWAY_PREFIX = "/gateway";

    String PATH = "/";

    /**
     * 注册路径 如果不存在
     *
     * @param path
     * @param value
     * @param isPersistent
     */
    void registerPathIfNotExists(String path, String value, boolean isPersistent) throws Exception;

    /**
     * 注册临时节点
     *
     * @param key
     * @param value
     * @return
     * @throws Exception
     */
    long registerEphemeralNode(String key, String value) throws Exception;

    /**
     * 注册一个永久节点
     *
     * @param key
     * @param value
     * @throws Exception
     */
    void registerPersistentNode(String key, String value) throws Exception;

    /**
     * 通过前缀路径 获取路径数据
     *
     * @param prefix
     * @return
     */
    List<Pair<String, String>> getListByPrefixKey(String prefix) throws Exception;

    /**
     * 查询一个key
     *
     * @param key
     * @return
     * @throws Exception
     */
    Pair<String, String> getByKey(String key) throws Exception;

    /**
     * 判断key是否存在
     *
     * @param key
     * @return
     * @throws Exception
     */
    boolean isExistKey(String key) throws Exception;

    /**
     * 根据key删除
     *
     * @param key
     * @throws Exception
     */
    void deleteByKey(String key) throws Exception;

    /**
     * 关闭服务
     */
    void close();
}
