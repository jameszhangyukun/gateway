package com.zyk.rapid.core.plugin;

public interface Plugin {
    default boolean check() {
        return true;
    }

    void init();

    void destroy();

    Plugin getPlugin(String pluginName);
}
