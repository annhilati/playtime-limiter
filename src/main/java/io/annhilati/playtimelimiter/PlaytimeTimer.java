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
import org.bukkit.permissions.PermissionAttachmentInfo;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Bukkit.broadcast(Component.text(event.getName() + " wants to join"));

        // Falls nicht in timerData vorhanden
        if (!playerData.isSet(uuid.toString())) {
            String defaultGroup = config.getString("default-group");

            playerData.set(uuid + ".time", config.getInt("groups." + defaultGroup + ".start-timer"));
            playerData.set(uuid + ".mode", config.getString("groups." + defaultGroup + ".start-mode"));
            playerData.set(uuid + ".restricted", false);
            playerData.set(uuid + ".cached-groups", Arrays.asList(defaultGroup));
        }

        plugin.getGroupRuleManager().checkRulesFor(uuid);

        long time = playerData.getInt(uuid + ".time");
        String mode = playerData.getString(uuid + ".mode");
        boolean restricted = playerData.getBoolean(uuid + ".restricted");
        
        if (time <= 0 && mode != "bypass") {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("§cZeit abgelaufen!")
            );
        }
        if (restricted == true) {
             event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("§cNicht erlaubt (Restricted)!")
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

    public boolean hasSession(UUID uuid) {
        if (latestCheckIns.containsKey(uuid)) {
            return true;
        }
        return false;
    }

    public void checkIn(UUID uuid) {
        plugin.getLogger().info("Check in " + uuid);
        latestCheckIns.put(uuid, Instant.now());

        if (playerData.getInt(uuid + ".time") <= plugin.getConfig().getInt("update-cycle")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {checkOut(uuid);}, playerData.getInt(uuid + ".time") * 20L + 1L);
        }
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
            updatePlayerGroups(player);
            checkOut(uuid);
            checkIn(uuid);
        }

    }

    private void updatePlayerGroups(Player player) {
        FileConfiguration config = plugin.getConfig();

        List<String> permittedGroups = player.getEffectivePermissions().stream().filter(info -> info.getPermission().startsWith("limiter.group.")).filter(PermissionAttachmentInfo::getValue).map(info -> info.getPermission().substring("limiter.group.".length())).collect(Collectors.toList());
        if (permittedGroups.size() == 0) {
            Bukkit.broadcast(Component.text("Liste ist 0 " + config.getString("default-group")));
            playerData.set(player.getUniqueId() + ".cached-groups", Arrays.asList(config.getString("default-group")));
            savePlayerDataToFile();
            return;
        }
        playerData.set(player.getUniqueId() + ".cached-groups", permittedGroups);
    }
}
