package io.annhilati.playtimelimiter;

import org.bukkit.plugin.java.JavaPlugin;
import io.annhilati.playtimelimiter.commands.Limiter;

public final class PlaytimeLimiter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("test").setExecutor(new Limiter());

        String pluginName = getPluginMeta().getName();
        String version = getPluginMeta().getVersion();
        String author = String.join(", ", getPluginMeta().getAuthors());

        // Schöne Startmeldung in die Konsole
        getLogger().info("=================================");
        getLogger().info(pluginName + " v" + version + " by " + author + " gestartet!");
        getLogger().info("=================================");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
