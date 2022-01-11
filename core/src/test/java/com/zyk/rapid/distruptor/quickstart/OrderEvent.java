package com.zyk.rapid.distruptor.quickstart;

public class OrderEvent {
    private long value;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void print() {
        System.out.println("father");
    }
}
