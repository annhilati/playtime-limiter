package io.annhilati.playtimelimiter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeTimer implements Listener {

    private final PlaytimeLimiter plugin;

    private final Map<UUID, Long> latestSessionBegins = new HashMap<>();
    private final Map<UUID, Long> inCycleAccumulatedTimes = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        // Scheduler: jede Minute Online-Zeit aktualisieren
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 5,
                20L * plugin.getConfig().getInt("update-cycle"));

        // EventListener registrieren
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                      Data Storage                                        │ 
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    public FileConfiguration timerData;
    private File timerDataFile;
                                         
    public void createTimerDataFile() {
        timerDataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!timerDataFile.exists()) {
                                        
            timerDataFile.getParentFile().mkdirs(); // Ordner erstellen, falls nötig

                                        
            plugin.saveResource("data.yml", false); // Default-Datei aus Jar kopieren }                       
        }
        timerData = YamlConfiguration.loadConfiguration(timerDataFile); //Instanzvariable befüllen
    }

    public void saveTimerData() {
        try { 
            timerData.save(timerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                          Timing                                          │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    // Spieler-Login
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        beginTiming(uuid);
    }

    // Spieler-Logout
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        endTiming(uuid);
    }

    public void beginTiming(UUID uuid) {
        Bukkit.broadcast(Component.text("Beginn timing " + uuid));
        latestSessionBegins.put(uuid, System.currentTimeMillis());
    }

    public void endTiming(UUID uuid) {
        Bukkit.broadcast(Component.text("End timing " + uuid));
        Long start = latestSessionBegins.get(uuid);
        if (start == null)
            return; // Null-Safe

        long duration = System.currentTimeMillis() - start;
        long accumulated = inCycleAccumulatedTimes.getOrDefault(uuid, 0L);
        inCycleAccumulatedTimes.put(uuid, accumulated + duration);

        latestSessionBegins.remove(uuid);
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                     Time Adjustment                                      │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    private void updateOnlineTimes() {

        FileConfiguration config = plugin.getConfig();

        for (UUID uuid : latestSessionBegins.keySet()) {
            endTiming(uuid);

            // Falls nicht in timerData vorhanden
            if (!timerData.isSet(uuid.toString() + ".time")) {
                String defaultGroup = config.getString("default-group");

                timerData.set(uuid + ".time",
                        config.getInt("groups." + defaultGroup + ".start-timer"));
                timerData.set(uuid + ".mode",
                        config.getString("groups." + defaultGroup + ".start-mode"));
            }

            // Zeit abziehen
            if (timerData.getString(uuid + ".mode") != "paused") {

                int oldTime = timerData.getInt(uuid + ".time");
                long latestSessionDuraion = inCycleAccumulatedTimes.getOrDefault(uuid, 0L);
                timerData.set(uuid + ".time", Math.max(0, oldTime - (int) latestSessionDuraion));
            }

            inCycleAccumulatedTimes.remove(uuid);

            // Kicken
            if (timerData.getInt(uuid + ".time") <= 0 && timerData.getString(uuid + ".mode") != "bypass") {

                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.kick(Component.text("§cZeit abgelaufen!"));
                }
            }
        }

        // Speichern
        saveTimerData();

        // Für alle Spieler neue Session starten
        for (Player player : Bukkit.getOnlinePlayers()) {
            beginTiming(player.getUniqueId());
        }
    }
}
