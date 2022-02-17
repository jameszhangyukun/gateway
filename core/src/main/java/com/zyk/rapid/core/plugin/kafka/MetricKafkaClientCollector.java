package com.zyk.rapid.core.plugin.kafka;

import com.zyk.gateway.common.metric.MetricClientCollector;
import com.zyk.gateway.common.metric.TimeSeries;
import com.zyk.gateway.common.util.FastJsonConvertUtil;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Kafka指标收集类
 */
public class MetricKafkaClientCollector implements MetricClientCollector {
    /**
     * 每个发送批的大小
     */
    private final int batchSize = 1024 * 16;
    /**
     * batch没满情况下默认等待100ms
     */
    private final int lingerMs = 100;


    private final int bufferMemory = 1024 * 1024 * 64;

    private final String compressionType = "lz4";

    private final int blockMs = 10000;
    private final String serializerClass = StringSerializer.class.getName();

    private final Properties properties;

    private final String acks = "1";


    private KafkaProducer<String, String> producer;

    public MetricKafkaClientCollector(String address) {
        this.properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, address);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        properties.put(ProducerConfig.ACKS_CONFIG, acks);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, blockMs);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializerClass);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializerClass);
    }

    public <T extends TimeSeries> RecordMetadata sendSync(String topic, T message) throws Exception {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(message);

        Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, FastJsonConvertUtil.convertObjectToJSON(message)));
        return future.get();
    }

    public <T extends TimeSeries> void sendAsync(String topic, T message) throws Exception {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(message);

        producer.send(new ProducerRecord<>(topic, FastJsonConvertUtil.convertObjectToJSON(message)));
    }

    public <T extends TimeSeries> void sendAsync(String topic, T message, Callback callback) throws Exception {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(message);

        producer.send(new ProducerRecord<>(topic, FastJsonConvertUtil.convertObjectToJSON(message)), callback);
    }

    @Override
    public void start() {
        this.producer = new KafkaProducer<>(this.properties);
    }


    @Override
    public void shutdown() {
        this.producer.close();
    }
}
