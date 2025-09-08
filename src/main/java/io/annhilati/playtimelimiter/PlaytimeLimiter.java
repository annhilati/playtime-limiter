package io.annhilati.playtimelimiter;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaytimeLimiter extends JavaPlugin {

    private PlaytimeTimer playtimeTimer;
    
    @Override
    public void onEnable() {
        
        // Config & Data
        saveDefaultConfig();
        
        playtimeTimer = new PlaytimeTimer(this);
        playtimeTimer.createTimerDataFile();
 
        // Command Registration
        Objects.requireNonNull(getCommand("limiter")).setExecutor(new LimiterCommand(this));
        Objects.requireNonNull(getCommand("limiter")).setTabCompleter(new LimiterCommandCompletion()); 

        // PlaceholderAPI-Integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaytimePlaceholder(this).register();
        }

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
    public PlaytimeTimer getPlaytimeTimer() {
        return playtimeTimer;
    }
}
