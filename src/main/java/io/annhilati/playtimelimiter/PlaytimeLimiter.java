package io.annhilati.playtimelimiter;

import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimeLimiter extends JavaPlugin {

    @Override
    public void onEnable() {
 
        Objects.requireNonNull(getCommand("limiter")).setExecutor(new LimiterCommand());

        String pluginName = getPluginMeta().getName();
        String version = getPluginMeta().getVersion();
        String author = String.join(", ", getPluginMeta().getAuthors());

        getLogger().info("=================================");
        getLogger().info(pluginName + " v" + version + " by " + author + " gestartet!");
        getLogger().info("=================================");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
