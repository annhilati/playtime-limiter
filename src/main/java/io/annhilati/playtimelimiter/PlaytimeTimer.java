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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlaytimeTimer implements Listener {

    private final PlaytimeLimiter plugin;

    public final Map<UUID, Instant> latestCheckIns = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        createPlayerDataFileDefault();

        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 5, 20L * plugin.getConfig().getInt("update-cycle"));

        Bukkit.getPluginManager().registerEvents(this, plugin); // Für Events, nicht für Scheduling
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                      Data Storage                                        │ 
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    public FileConfiguration playerData;
    private File playerDataFile;
                                         
    public void createPlayerDataFileDefault() {
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
        FileConfiguration config = plugin.getConfig();

        // Falls nicht in timerData vorhanden
        if (!playerData.isSet(uuid.toString())) {
            String defaultGroup = config.getString("default-group");

            playerData.set(uuid + ".time", config.getInt("groups." + defaultGroup + ".start-timer"));
            playerData.set(uuid + ".mode", config.getString("groups." + defaultGroup + ".start-mode"));
        }

        long time = playerData.getInt(uuid + ".time");
        String mode = playerData.getString(uuid + ".mode");
        
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
        plugin.getLogger().info("Check in " + uuid);
        latestCheckIns.put(uuid, Instant.now());
    }

    public void checkOut(UUID uuid) {
        plugin.getLogger().info("Check out " + uuid);
        Instant checkIn = latestCheckIns.get(uuid);
        if (checkIn == null)
            return; // Null-Safe

        Duration duration = Duration.between(checkIn, Instant.now());

        // Zeit abziehen
        if (!Objects.equals(playerData.getString(uuid + ".mode"), "paused")) {

            int oldTime = playerData.getInt(uuid + ".time");
            playerData.set(uuid + ".time", Math.max(0, oldTime - duration.getSeconds()));
        }

        // Kicken
        if (playerData.getInt(uuid + ".time") <= 0 && !Objects.equals(playerData.getString(uuid + ".mode"), "bypass")) {

            Bukkit.getPlayer(uuid).kick(Component.text("§cZeit abgelaufen!"));
        }

        latestCheckIns.remove(uuid);
        savePlayerDataToFile();

    }

    private void updateOnlineTimes() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            checkOut(uuid);
            checkIn(uuid);
        }

    }
}
