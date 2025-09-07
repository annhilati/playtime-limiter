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

    private FileConfiguration timerData; // Instanzvariable
    private File timerDataFile; // Datei-Referenz

    private void createTimerDataFile() {
        timerDataFile = new File(getDataFolder(), "data.yml");
        if (!timerDataFile.exists()) {
            timerDataFile.getParentFile().mkdirs(); // Ordner erstellen, falls nötig
            saveResource("data.yml", false);        // Default-Datei aus Jar kopieren
        }
        timerData = YamlConfiguration.loadConfiguration(timerDataFile); // Instanzvariable befüllen
    }

    public FileConfiguration getTimerData() {
        return timerData; // Zugriff von außen
    }

    public void saveTimerData() {
        try {
            timerData.save(timerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Hilfklassenzugriff
    private PlaytimeTimer playtimeTimer;

    public PlaytimeTimer getPlayertimeTimer() {
        return playtimeTimer;
    }
}
