package io.annhilati.playtimelimiter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeTimer implements Listener {

    private final PlaytimeLimiter plugin;

    public final Map<UUID, Long> latestSessionBegins = new HashMap<>();
    public final Map<UUID, Long> inCycleAccumulatedTimes = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        // Scheduler: jede Minute Online-Zeit aktualisieren
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 5, 20L * plugin.getConfig().getInt("update-cycle"));

        // EventListener registrieren
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Spieler-Login
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        beginSession(uuid);
    }

    // Spieler-Logout
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        endSession(uuid);
    }

    public void beginSession(UUID uuid) {
        Bukkit.broadcast(Component.text("Beginn session " + uuid));
        latestSessionBegins.put(uuid, System.currentTimeMillis());
    }

    public void endSession(UUID uuid) {
        Bukkit.broadcast(Component.text("End session " + uuid));
        long latestSessionTime = System.currentTimeMillis() - latestSessionBegins.get(uuid);
        long alreadyAccumulatedTime = inCycleAccumulatedTimes.getOrDefault(uuid, 0L);
        inCycleAccumulatedTimes.put(uuid, alreadyAccumulatedTime + latestSessionTime);

        latestSessionBegins.remove(uuid);
    }

    // Minütliche Aktualisierung
    private void updateOnlineTimes() {

        FileConfiguration config = plugin.getConfig();
        FileConfiguration timerData = plugin.getTimerData();
        
        for (UUID uuid : latestSessionBegins.keySet()) {
            endSession(uuid);
        
            // Allerlei Logik mit inCycleAccumulatedTimes
            
            // Spieler ist nicht in TimerData
            if (timerData.get(uuid.toString() + ".time") == null) {
                
                String default_group = plugin.getConfig().getString("default-group");
                
                timerData.set(uuid + ".time", config.getInt("groups." + default_group + ".start-timer"));
                timerData.set(uuid + ".mode", config.getString("groups." + default_group + ".start-mode"));
            }
            
            // Zeit aktualisieren
            timerData.set(uuid + ".time", timerData.getInt(uuid + ".time") - inCycleAccumulatedTimes.get(uuid));
            
            // Falls Zeit abgelaufen und kein bypass
            if (timerData.getInt(uuid + ".time") <= 0 && timerData.getString(uuid + ".mode") != "bypass") {
                Bukkit.getPlayer(uuid).kick(Component.text("Zeit abgelaufen"));
                timerData.set(uuid + ".time", 0);
            }
            
        }
        
        plugin.saveTimerData();
        Bukkit.broadcast(Component.text("Cycle" + " " + timerData.get(UUID.fromString("d0dfb0e8-fd78-471e-a566-a4ad2d404594") + ".time")));

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            beginSession(uuid);
        }
    }
}
