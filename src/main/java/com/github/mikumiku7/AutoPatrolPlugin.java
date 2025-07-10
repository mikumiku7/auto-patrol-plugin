package com.github.mikumiku7;

import com.github.mikumiku7.command.AutoPatrolCommand;
import com.github.mikumiku7.module.AutoPatrol;
import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

@Plugin(
        id = "auto-patrol-plugin",
        version = BuildConstants.VERSION,
        description = "ZenithProxy AutoPatrol Plugin",
        url = "https://github.com/mikumiku7",
        authors = {"mikumiku7"},
        mcVersions = {"1.21.4"} // to indicate any MC version: @Plugin(mcVersions = "*")
        // if you touch packet classes, you almost certainly need to pin to a single mc version
)
public class AutoPatrolPlugin implements ZenithProxyPlugin {
    // public static for simple access from modules and commands
    // or alternatively, you could pass these around in constructors
    public static AutoPatrolConfig PLUGIN_CONFIG;
    public static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        LOG.info("AutoPatrol Plugin loading...");
        // initialize any configurations before modules or commands might need to read them
        PLUGIN_CONFIG = pluginAPI.registerConfig("auto-patrol", AutoPatrolConfig.class);
        pluginAPI.registerModule(new AutoPatrol());

        pluginAPI.registerCommand(new AutoPatrolCommand());
        LOG.info("AutoPatrol Plugin loaded!");
    }
}
