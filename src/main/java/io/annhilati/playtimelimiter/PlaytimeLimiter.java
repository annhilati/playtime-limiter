package io.annhilati.playtimelimiter;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimeLimiter extends JavaPlugin {

    @Override
    public void onEnable() {

        // Config & Data
        saveDefaultConfig();
        createTimerDataFile();
        playtimeTimer = new PlaytimeTimer(this);
 
        // Command Registration
        Objects.requireNonNull(getCommand("limiter")).setExecutor(new LimiterCommand(this));
        Objects.requireNonNull(getCommand("limiter")).setTabCompleter(new LimiterCommandCompletion()); 

        // Logging
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

    


    // Hilfklassenzugriff
    private PlaytimeTimer playtimeTimer;

    public PlaytimeTimer getPlaytimeTimer() {
        return playtimeTimer;
    }
}
