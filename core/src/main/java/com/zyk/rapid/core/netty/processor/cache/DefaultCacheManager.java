package com.zyk.rapid.core.netty.processor.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultCacheManager {

    public static final String FILTER_CONFIG_CACHE_ID = "filterConfigCache";

    private DefaultCacheManager() {

    }

    /**
     * 全局缓存：
     */
    private final ConcurrentHashMap<String, Cache<String, ?>> cacheMap = new ConcurrentHashMap<>();

    static class SingletonHolder {
        private static final DefaultCacheManager INSTANCE = new DefaultCacheManager();
    }

    public static DefaultCacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 根据全局缓存id创建缓存对象
     *
     * @param cacheId
     * @param <V>
     * @return
     */
    public <V> Cache<String, V> create(String cacheId) {
        Cache<String, V> cache = Caffeine.newBuilder().build();
        cacheMap.put(cacheId, cache);
        return cache;
    }

    /**
     * 根据cacheId和真实key删除Caffeine缓存对象
     *
     * @param cacheId
     * @param key
     * @param <V>
     */
    public <V> void remove(String cacheId, String key) {
        Cache<String, V> cache = (Cache<String, V>) cacheMap.get(cacheId);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 根据全局缓存Id删除缓存
     *
     * @param cacheId
     * @param <V>
     */
    public <V> void remove(String cacheId) {
        Cache<String, V> cache = (Cache<String, V>) cacheMap.get(cacheId);
        if (cache != null) {
            cache.invalidateAll();
        }
    }


    public void cleanAll() {
        cacheMap.values().forEach(Cache::invalidateAll);
    }

    public static Cache<String, GenericService> createCacheForDubboGenericService() {
        return Caffeine.newBuilder().build();
    }
}
