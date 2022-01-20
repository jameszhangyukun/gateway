package com.zyk.rapid.etcd.test;

import com.zyk.rapid.etcd.core.EtcdClientImpl;
import io.etcd.jetcd.KeyValue;

import java.nio.charset.Charset;

public class EtcdImplTest {
    public static void main(String[] args) throws Exception {
        String registryAddress = "http://127.0.0.1:2379";
        EtcdClientImpl etcdClient = new EtcdClientImpl(registryAddress);
        etcdClient.putKey("/aaa", "bbb");
        KeyValue keyValue = etcdClient.getKey("/aaa");
        System.out.println("key:" + keyValue.getKey().toString(Charset.defaultCharset()) +",value:"+ keyValue.getValue().toString(Charset.defaultCharset()));
        etcdClient.deleteKey("aaa");
    }
}
