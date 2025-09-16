package io.annhilati.playtimelimiter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlaytimeTimer implements Listener {

    private final PlaytimeLimiter plugin;

    private final Map<UUID, Long> latestSessionBegins = new HashMap<>();
    private final Map<UUID, Long> inCycleAccumulatedTimes = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 5,
                20L * plugin.getConfig().getInt("update-cycle"));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                      Data Storage                                        │ 
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    public FileConfiguration playerData;
    private File playerDataFile;
                                         
    public void createTimerDataFile() {
        playerDataFile = new File(plugin.getDataFolder(), "playtime-data.yml");
        if (!playerDataFile.exists()) {
                                        
            playerDataFile.getParentFile().mkdirs(); // Ordner erstellen, falls nötig

                                        
            plugin.saveResource("playtime-data.yml", false); // Default-Datei aus Jar kopieren }                       
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile); //Instanzvariable befüllen
    }

    public void saveTimerData() {
        try { 
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                          Timing                                          │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        long time = playerData.getInt(uuid + ".time");
        
        if (time <= 0 && !Objects.equals(playerData.getString(uuid + ".time"), "bypass")) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("§cZeit abgelaufen!")
            );
        }
    }

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
        plugin.getLogger().info("Beginn timing " + uuid);
        latestSessionBegins.put(uuid, System.currentTimeMillis());
    }

    public void endTiming(UUID uuid) {
        plugin.getLogger().info("End timing " + uuid);
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
            if (!playerData.isSet(uuid.toString() + ".time")) {
                String defaultGroup = config.getString("default-group");

                playerData.set(uuid + ".time",
                        config.getInt("groups." + defaultGroup + ".start-timer"));
                playerData.set(uuid + ".mode",
                        config.getString("groups." + defaultGroup + ".start-mode"));
            }

            // Zeit abziehen
            if (!Objects.equals(playerData.getString(uuid + ".mode"), "paused")) {

                int oldTime = playerData.getInt(uuid + ".time");
                long latestSessionDuraion = inCycleAccumulatedTimes.getOrDefault(uuid, 0L);
                playerData.set(uuid + ".time", Math.max(0, oldTime - (int) latestSessionDuraion));
            }

            inCycleAccumulatedTimes.remove(uuid);

            // Kicken
            if (playerData.getInt(uuid + ".time") <= 0 && !Objects.equals(playerData.getString(uuid + ".mode"), "bypass")) {

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
