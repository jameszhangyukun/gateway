package com.zyk.rapid.core.netty.processor.filter.post;

import com.zyk.gateway.common.constants.ProcessFilterConstants;
import com.zyk.gateway.common.metric.Metric;
import com.zyk.gateway.common.metric.MetricType;
import com.zyk.gateway.common.util.Pair;
import com.zyk.gateway.common.util.TimeUtil;
import com.zyk.rapid.core.GatewayConfig;
import com.zyk.rapid.core.GatewayConfigLoader;
import com.zyk.rapid.core.balance.LoadBalance;
import com.zyk.rapid.core.context.Context;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.Filter;
import com.zyk.rapid.core.netty.processor.filter.FilterConfig;
import com.zyk.rapid.core.netty.processor.filter.ProcessorFilterType;
import com.zyk.rapid.core.plugin.Plugin;
import com.zyk.rapid.core.plugin.PluginManager;
import com.zyk.rapid.core.plugin.kafka.MetricKafkaClientCollector;
import com.zyk.rapid.core.plugin.kafka.MetricKafkaClientPlugin;
import com.zyk.rapid.core.rolling.RollingNumber;
import com.zyk.rapid.core.rolling.RollingNumberEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 后置过滤器
 */
@Filter(id = ProcessFilterConstants.STATISTIC_POST_FILTER_ID,
        value = ProcessorFilterType.POST,
        order = ProcessFilterConstants.STATISTIC_POST_FILTER_ORDER,
        name = ProcessFilterConstants.STATISTIC_POST_FILTER_NAME
)
public class StatisticPostFilter extends AbstractEntryProcessorFilter<StatisticPostFilter.Config> {
    public static final int windowSize = 60 * 1000;

    public static final int bucketSize = 60;

    private RollingNumber rollingNumber;

    private Thread consumerThread;


    public StatisticPostFilter() {
        super(StatisticPostFilter.Config.class);
        MetricConsumer metricConusmer = new MetricConsumer();
        this.rollingNumber = new RollingNumber(windowSize,
                bucketSize,
                "Rapid-Gateway",
                metricConusmer.getMetricQueue());
        consumerThread = new Thread(metricConusmer);
    }

    @Override
    public void entry(Context context, Object... args) throws Throwable {
        try {
            StatisticPostFilter.Config config = (Config) args[0];
            if (config.isRollingNumber()) {
                consumerThread.start();
                rollingNumber(context, args);
            }
        } finally {
            // 如果是最后一个filter
            context.terminated();
            super.fireNext(context, args);
        }
    }

    private void rollingNumber(Context context, Object... args) {
        Throwable throwable = context.getThrowable();
        if (throwable == null) {
            rollingNumber.increment(RollingNumberEvent.SUCCESS);
        } else {
            rollingNumber.increment(RollingNumberEvent.FAILURE);
        }
//	请求开始的时间
        long SRTime = context.getSRTime();
        //	路由的开始时间(route ---> service)
        long RSTime = context.getRSTime();
        //	路由的接收请求时间（service --> route）
        long RRTime = context.getRRTime();
        //	请求结束（写出请求的时间）
        long SSTime = context.getSSTime();

        //	整个生命周期的耗时
        long requestTimeout = SSTime - SRTime;
        long defaultRequestTimeout = GatewayConfigLoader.getGatewayConfig().getHttpRequestTimeout();
        if (requestTimeout > defaultRequestTimeout) {
            rollingNumber.increment(RollingNumberEvent.REQUEST_TIMEOUT);
        }

        long routeTimeout = RRTime - RSTime;
        long defaultRouteTimeout = GatewayConfigLoader.getGatewayConfig().getHttpRequestTimeout();
        if (routeTimeout > defaultRouteTimeout) {
            rollingNumber.increment(RollingNumberEvent.ROUTE_TIMEOUT);
        }
    }

    @Getter
    @Setter
    public static class Config extends FilterConfig {
        private boolean rollingNumber = true;
    }

    public class MetricConsumer implements Runnable {

        private ArrayBlockingQueue<Pair<String, Long>> metricQueue = new ArrayBlockingQueue<>(65535);

        private volatile boolean isRunning = false;

        public void start() {
            isRunning = true;
        }

        public void shutdown() {
            isRunning = false;
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Pair<String, Long> pair = metricQueue.take();
                    String key = pair.getObject1();
                    Long value = pair.getObject2();

                    Plugin plugin = PluginManager.getPlugin().getPlugin(MetricKafkaClientPlugin.class.getName());
                    if (plugin != null) {
                        MetricKafkaClientPlugin metricKafkaClientPlugin = (MetricKafkaClientPlugin) plugin;
                        HashMap<String, String> tags = new HashMap<>();

                        tags.put(MetricType.KEY, MetricType.STATISTICS);
                        String topic = GatewayConfigLoader.getGatewayConfig().getMetricTopic();
                        Metric metric = Metric.create(key, value, TimeUtil.currentTimeMillis(), tags, topic, false);
                        metricKafkaClientPlugin.send(metric);
                    }


                } catch (Exception e) {

                }
            }
        }

        public ArrayBlockingQueue<Pair<String, Long>> getMetricQueue() {
            return metricQueue;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }
}
