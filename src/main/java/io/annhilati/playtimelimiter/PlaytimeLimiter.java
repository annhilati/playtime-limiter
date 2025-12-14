package io.annhilati.playtimelimiter;

import io.annhilati.playtimelimiter.Rules.GroupRuleManager;

import java.util.Objects;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlaytimeLimiter extends JavaPlugin {

    private PlaytimeTimer playtimeTimer;
    private GroupRuleManager groupRuleManager;
    private File messageConfigFile;
    private YamlConfiguration messageConfig;
    
    @Override
    public void onEnable() {
        
        // Config & Data
        saveDefaultConfig();
        
        playtimeTimer = new PlaytimeTimer(this);
 
        // Regel-Manager laden
        groupRuleManager = new GroupRuleManager(this);
        
        // PlaceholderAPI-Integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaytimePlaceholder(this).register();
        }
        
        // Instanciate message configuration
        messageConfigFile = new File(this.getDataFolder(), "messages.yml");
        if (!messageConfigFile.exists()) {
            
            messageConfigFile.getParentFile().mkdirs();
            
            this.saveResource("messages.yml", false);
        }
        this.messageConfig = YamlConfiguration.loadConfiguration(messageConfigFile);
        
        // Command Registration
        Objects.requireNonNull(getCommand("limiter")).setExecutor(new LimiterCommand(this));
        Objects.requireNonNull(getCommand("limiter")).setTabCompleter(new LimiterCommandCompletion()); 

        // Logging
        String pluginName = getPluginMeta().getName();
        String version = getPluginMeta().getVersion();
        String author = String.join(", ", getPluginMeta().getAuthors());

        getLogger().info("╭──────────────────────────────────────────────────────────────────────────────╮");
        getLogger().info("│ " + pluginName + " v" + version + " by " + author + " gestartet!");
        getLogger().info("╰──────────────────────────────────────────────────────────────────────────────╯");

    }

    public void reload() {
        this.reloadConfig();
        this.messageConfig = YamlConfiguration.loadConfiguration(messageConfigFile);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Hilfklassenzugriff
    public YamlConfiguration getMessageConfig() {
        return messageConfig;
    }
    public PlaytimeTimer getPlaytimeTimer() {
        return playtimeTimer;
    }
    public GroupRuleManager getGroupRuleManager() {
        return groupRuleManager;
    }
}
