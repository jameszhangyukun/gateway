package com.zyk.rapid.core;

import com.lmax.disruptor.*;
import com.zyk.gateway.common.constants.BasicConst;
import com.zyk.gateway.common.constants.RapidBufferHelper;
import com.zyk.gateway.common.util.NetUtils;
import lombok.Data;

/**
 * 网关的通用配置类
 */
@Data
public class GatewayConfig {
    /**
     * 网关的默认端口
     */
    private int port = 8888;

    /**
     * 网关的唯一id
     */
    private String gatewayId = NetUtils.getLocalIp() + BasicConst.COLON_SEPARATOR + port;
    /**
     * 注册中心的地址
     */
    private String registryAddress = "http://127.0.0.1:2379";

    /**
     * 网关的命名空间
     */
    private String namespace = "gateway-dev";

    private String env = "dev";
    /**
     * 网关服务器的CPU核数
     */
    private int processThread = Runtime.getRuntime().availableProcessors();
    // 	Netty的Boss线程数
    private int eventLoopGroupBossNum = 1;

    //	Netty的Work线程数
    private int eventLoopGroupWorkNum = processThread;

    //	是否开启EPOLL
    private boolean useEPoll = true;

    //	是否开启Netty内存分配机制
    private boolean nettyAllocator = true;

    //	http body报文最大大小
    private int maxContentLength = 64 * 1024 * 1024;

    private String kafkaAddress;

    //	dubbo开启连接数数量
    private int dubboConnections = processThread;

    //	设置响应模式, 默认是单异步模式：CompletableFuture回调处理结果： whenComplete  or  whenCompleteAsync
    private boolean whenComplete = true;

    //	网关队列配置：缓冲模式；
    private String bufferType = RapidBufferHelper.FLUSHER;
    ; // RapidBufferHelper.FLUSHER;

    //	网关队列：内存队列大小
    private int bufferSize = 1024 * 16;

    //	网关队列：阻塞/等待策略
    private String waitStrategy = "blocking";
    private String metricTopic = "gateway-topic";

    public WaitStrategy getATrueWaitStrategy() {
        switch (waitStrategy) {
            case "busySpin":
                return new BusySpinWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "sleeping":
                return new SleepingWaitStrategy();
            default:
                return new BlockingWaitStrategy();
        }
    }

    /**
     * Netty的Boss线程数
     */
    private int eventLLoopGroupBossNum = 1;

    // HTTP async 参数选项
    /**
     * 连接超时时间
     */
    private int httpConnectTime = 30 * 1000;

    /**
     * 请求超时时间
     */
    private int httpRequestTimeout = 30 * 1000;
    /**
     * 最大重试次数
     */
    private int httpMaxRequestRetry = 2;

    /**
     * 最大连接数
     */
    private int httpMaxConnections = 10000;
    /**
     * 客户端每个地址支持的最大连接数
     */
    private int httpConnectionPerHost = 8000;
    /**
     * 客户端空闲连接超时事件，默认是60s
     */
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    public String getMetricTopic() {
        return metricTopic;
    }
}
