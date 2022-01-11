package com.zyk.rapid.distruptor.quickstart;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.zyk.rapid.core.netty.processor.filter.AbstractEntryProcessorFilter;
import com.zyk.rapid.core.netty.processor.filter.pre.TimeoutPreFilter;
import org.apache.dubbo.rpc.filter.TimeoutFilter;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadFactory;


/**
 * disruptor
 */
public class Main {
    public static void main(String[] args) {
        OrderEvent orderEvent = new OrderEventImpl();
        orderEvent.print();
    }
}
