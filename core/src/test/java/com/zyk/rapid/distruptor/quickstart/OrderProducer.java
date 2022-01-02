package com.zyk.rapid.distruptor.quickstart;

import com.lmax.disruptor.RingBuffer;

import java.nio.ByteBuffer;

public class OrderProducer {
    private final RingBuffer<OrderEvent> ringBuffer;

    public OrderProducer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void sendData(ByteBuffer byteBuffer) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent orderEvent = ringBuffer.get(sequence);
            orderEvent.setValue(byteBuffer.getLong(0));
        } finally {
            ringBuffer.publish(sequence);
        }

    }
}
