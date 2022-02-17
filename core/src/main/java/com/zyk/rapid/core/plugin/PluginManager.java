package com.zyk.rapid.core.plugin;

import com.zyk.gateway.common.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PluginManager {
    private final MultiplePluginImpl multiplePlugin;

    private static final PluginManager INSTANCE = new PluginManager();

    public static Plugin getPlugin() {
        return INSTANCE.multiplePlugin;
    }

    private PluginManager() {
        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class);
        Map<String, Plugin> pluginMap = new HashMap<>();
        for (Plugin plugin : plugins) {
            if (plugin.check()) {
                String pluginName = plugin.getClass().getName();
                pluginMap.put(pluginName, plugin);
                log.info("#PluginFactory# The Scanner Plugin is：{}", pluginName);
            }
        }
        this.multiplePlugin = new MultiplePluginImpl(pluginMap);

        Runtime.getRuntime().addShutdownHook(new Thread(multiplePlugin::destroy, "Shutdown-plugin"));
    }
}
