package com.zyk.rapid.distruptor.quickstart;

public class OrderEventImpl extends OrderEvent {
    @Override
    public void print() {
        System.out.println("child");
    }
}
