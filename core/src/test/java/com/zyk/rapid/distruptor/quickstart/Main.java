package com.zyk.rapid.distruptor.quickstart;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;


/**
 * disruptor
 */
public class Main {
    public static void main(String[] args) {
        int ringBufferSize = 1024 * 1024;
        Disruptor<OrderEvent> disruptor = new Disruptor<>(new OrderEventFactory(),
                ringBufferSize,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ds-thread");
                    return new Thread(r);
                },
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );
        disruptor.handleEventsWith(new OrderEventHandler());

        disruptor.start();
        OrderProducer orderProducer = new OrderProducer(disruptor.getRingBuffer());
        for(long i = 0;i < 100;i ++)
        {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong(i);
            orderProducer.sendData(byteBuffer);
        }
        disruptor.shutdown();
    }
}
