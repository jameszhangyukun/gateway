package com.zyk.rapid.core.rolling;

import lombok.Getter;

/**
 * RollingNumber事件
 */
public enum RollingNumberEvent {
    SUCCESS(1, 1),
    FAILURE(1, 2),
    REQUEST_TIMEOUT(1, 3),
    ROUTE_TIMEOUT(1, 4);


    private final int type;
    @Getter
    private final int name;

    RollingNumberEvent(int type, int name) {
        this.type = type;
        this.name = name;
    }

    public boolean isCounter() {
        return type == 1;
    }

    public boolean isMaxUpdater() {
        return type == 2;
    }
}
