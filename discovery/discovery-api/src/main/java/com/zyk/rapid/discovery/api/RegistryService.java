package com.zyk.rapid.discovery.api;

/**
 * 注册服务接口
 */
public interface RegistryService extends Registry {
    /**
     * 添加监听事件
     *
     * @param superPath 父节点目录
     * @param notify    监听函数
     */
    void addWatcherListeners(String superPath, Notify notify);

    /**
     * 初始化注册服务
     *
     * @param registryAddress
     */
    void initialized(String registryAddress);
}
