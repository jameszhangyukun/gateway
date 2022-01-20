package com.zyk.rapid.discovery;

import com.zyk.gateway.common.util.Pair;
import com.zyk.rapid.discovery.api.Notify;
import com.zyk.rapid.discovery.api.Registry;
import com.zyk.rapid.discovery.api.RegistryService;
import com.zyk.rapid.etcd.api.EtcdChangedEvent;
import com.zyk.rapid.etcd.api.EtcdClient;
import com.zyk.rapid.etcd.api.HeartBeatLeaseTimeoutListener;
import com.zyk.rapid.etcd.api.WatcherListener;
import com.zyk.rapid.etcd.core.EtcdClientImpl;
import io.etcd.jetcd.KeyValue;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册中心实现类
 */
@Slf4j
public class EtcdRegistryServiceImpl implements RegistryService {

    private EtcdClient etcdClient;

    private Map<String, String> cachedMap = new HashMap<>();


    @Override
    public void initialized(String registryAddress) {
        // 初始化Etcd客户端
        etcdClient = new EtcdClientImpl(registryAddress,
                true,
                "",
                null,
                null,
                null);

        // 添加异常的过期处理监听
        etcdClient.addHeartBeatLeaseTimeoutNotifyListener(new HeartBeatLeaseTimeoutListener() {
            @Override
            public void timeoutNotify() {
                cachedMap.forEach((key, value) -> {
                    try {
                        registerEphemeralNode(key, value);
                    } catch (Exception e) {
                        log.error("#EtcdRegistryServiceImpl.initialized# HeartBeatLeaseTimeoutListener: timeoutNotify is error", e);
                    }
                });
            }
        });
    }

    @Override
    public void registerPathIfNotExists(String path, String value, boolean isPersistent) throws Exception {
        if (!isExistKey(path)) {
            if (isPersistent) {
                registerPersistentNode(path, value);
            } else {
                registerEphemeralNode(path, value);
            }
        }
    }

    @Override
    public long registerEphemeralNode(String key, String value) throws Exception {
        long leaseId = this.etcdClient.getHeartBeatLeaseId();
        cachedMap.put(key, value);
        return this.etcdClient.putKeyWithLeaseId(key, value, leaseId);
    }

    @Override
    public void registerPersistentNode(String key, String value) throws Exception {
        this.etcdClient.putKey(key, value);
    }

    @Override
    public List<Pair<String, String>> getListByPrefixKey(String prefix) throws Exception {
        List<KeyValue> keyValues = this.etcdClient.getKeyWithPrefix(prefix);
        List<Pair<String, String>> result = new ArrayList<>(keyValues.size());
        for (KeyValue keyValue : keyValues) {
            result.add(new Pair<>(keyValue.getKey().toString(Charset.defaultCharset()), keyValue.getValue().toString(Charset.defaultCharset())));
        }
        return result;
    }

    @Override
    public Pair<String, String> getByKey(String key) throws Exception {
        KeyValue keyValue = this.etcdClient.getKey(key);
        return new Pair<>(keyValue.getKey().toString(Charset.defaultCharset()), keyValue.getValue().toString(Charset.defaultCharset()));
    }

    @Override
    public boolean isExistKey(String key) throws Exception {
        KeyValue keyValue = etcdClient.getKey(key);
        return keyValue != null;
    }

    @Override
    public void deleteByKey(String key) throws Exception {
        etcdClient.deleteKey(key);
    }

    @Override
    public void close() {
        if (etcdClient != null) {
            etcdClient.close();
        }
    }

    @Override
    public void addWatcherListeners(String superPath, Notify notify) {
        etcdClient.addWatcherListener(superPath + Registry.SERVICE_PREFIX, true, new InnerWatcherListener(notify));
        etcdClient.addWatcherListener(superPath + Registry.RULE_PREFIX, true, new InnerWatcherListener(notify));
        etcdClient.addWatcherListener(superPath + Registry.INSTANCE_PREFIX, true, new InnerWatcherListener(notify));
        etcdClient.addWatcherListener(superPath + Registry.GATEWAY_PREFIX, true, new InnerWatcherListener(notify));


    }

    static class InnerWatcherListener implements com.zyk.rapid.etcd.api.WatcherListener {
        private final Notify notify;

        public InnerWatcherListener(Notify notify) {
            this.notify = notify;
        }

        @Override
        public void watcherKeyChanged(EtcdClient etcdClient, EtcdChangedEvent event) throws Exception {
            EtcdChangedEvent.Type type = event.getType();
            KeyValue current = event.getCurtkeyValue();
            switch (type) {
                case PUT:
                    notify.put(current.getKey().toString(Charset.defaultCharset()), current.getValue().toString(Charset.defaultCharset()));
                    break;
                case DELETE:
                    notify.delete(current.getKey().toString(Charset.defaultCharset()));
                    break;
                default:
                    break;
            }
        }
    }

}
