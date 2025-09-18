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

    private final Map<UUID, Long> latestCheckIns = new HashMap<>();
    private final Map<UUID, Long> afterCheckInAccumulatedDurations = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        createPlayerDataDefaultFile();

        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 5, 20L * plugin.getConfig().getInt("update-cycle"));

        Bukkit.getPluginManager().registerEvents(this, plugin); // Für Events, nicht für Scheduling
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                      Data Storage                                        │ 
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    public FileConfiguration playerData;
    private File playerDataFile;
                                         
    public void createPlayerDataDefaultFile() {
        playerDataFile = new File(plugin.getDataFolder(), "player-data.yml");
        if (!playerDataFile.exists()) {
                                        
            playerDataFile.getParentFile().mkdirs();
                                        
            plugin.saveResource("player-data.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void savePlayerDataToFile() {
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
        
        if (time <= 0 && !Objects.equals(playerData.getString(uuid + ".mode"), "bypass")) {
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
        checkIn(uuid);
    }

    // Spieler-Logout
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        checkOut(uuid);
    }

    public void checkIn(UUID uuid) {
        plugin.getLogger().info("Beginn timing " + uuid);
        latestCheckIns.put(uuid, System.currentTimeMillis());
    }

    public void checkOut(UUID uuid) {
        plugin.getLogger().info("End timing " + uuid);
        Long start = latestCheckIns.get(uuid);
        if (start == null)
            return; // Null-Safe

        long duration = System.currentTimeMillis() - start;
        long accumulated = afterCheckInAccumulatedDurations.getOrDefault(uuid, 0L);
        afterCheckInAccumulatedDurations.put(uuid, accumulated + duration);

        latestCheckIns.remove(uuid);
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                     Time Adjustment                                      │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    private void updateOnlineTimes() {

        FileConfiguration config = plugin.getConfig();

        for (UUID uuid : latestCheckIns.keySet()) {
            checkOut(uuid);

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
                long latestSessionDuraion = afterCheckInAccumulatedDurations.getOrDefault(uuid, 0L);
                playerData.set(uuid + ".time", Math.max(0, oldTime - (int) latestSessionDuraion));
            }

            afterCheckInAccumulatedDurations.remove(uuid);

            // Kicken
            if (playerData.getInt(uuid + ".time") <= 0 && !Objects.equals(playerData.getString(uuid + ".mode"), "bypass")) {

                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.kick(Component.text("§cZeit abgelaufen!"));
                }
            }
        }

        // Speichern
        savePlayerDataToFile();

        // Für alle Spieler neue Session starten
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkIn(player.getUniqueId());
        }
    }
}
