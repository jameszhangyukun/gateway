package com.zyk.rapid.core.plugin.kafka;

import com.zyk.gateway.common.metric.TimeSeries;
import com.zyk.rapid.core.GatewayConfigLoader;
import com.zyk.rapid.core.plugin.Plugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MetricKafkaClientPlugin implements Plugin {

    private MetricKafkaClientCollector metricKafkaClientCollector;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private String address;

    public MetricKafkaClientPlugin() {

    }

    @Override
    public boolean check() {
        this.address = GatewayConfigLoader.getGatewayConfig().getKafkaAddress();
        return !StringUtils.isEmpty(this.address);
    }

    @Override
    public void init() {
        if (check()) {
            this.metricKafkaClientCollector = new MetricKafkaClientCollector(this.address);
            this.metricKafkaClientCollector.start();
            this.initialized.compareAndSet(false, true);
        }
    }

    @Override
    public void destroy() {
        if (checkInit()) {
            this.metricKafkaClientCollector.shutdown();
            this.initialized.compareAndSet(true, false);
        }
    }

    public <T extends TimeSeries> void send(T metric) {
        try {
            if (checkInit()) {
                metricKafkaClientCollector.sendAsync(metric.getDestination(), metric, (metadata, e) -> {
                    if (e != null) {
                        log.error("error", e);
                    }
                });
            }
        } catch (Exception e) {

        }
    }

    public <T extends TimeSeries> void sendBatch(List<T> metricList) {
        for (T metric : metricList) {
            send(metric);
        }
    }

    private boolean checkInit() {
        return this.initialized.get() && this.metricKafkaClientCollector != null;
    }

    @Override
    public Plugin getPlugin(String pluginName) {
        if (checkInit() && (MetricKafkaClientPlugin.class.getName().equals(pluginName))) {
            return this;
        }
        throw new RuntimeException("#MetricKafkaClientPlugin pluginName：#" + pluginName + "is not matched");
    }
}
