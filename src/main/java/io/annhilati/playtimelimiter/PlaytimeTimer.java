package io.annhilati.playtimelimiter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeTimer implements Listener {

    private final PlaytimeLimiter plugin;

    // Login-Zeit pro Spieler
    private final Map<UUID, Long> latestSessionBeginTimes = new HashMap<>();
    // Gesamtzeit pro Spieler
    private final Map<UUID, Long> inCycleAccumulatedTimes = new HashMap<>();

    public PlaytimeTimer(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        // Scheduler: jede Minute Online-Zeit aktualisieren
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateOnlineTimes, 20L * 60, 20L * 60);

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

    private void beginSession(UUID uuid) {
        latestSessionBeginTimes.put(uuid, System.currentTimeMillis());
    }

    private void endSession(UUID uuid) {
        long latestSessionTime = System.currentTimeMillis() - latestSessionBeginTimes.get(uuid);
        long alreadyAccumulatedTime = inCycleAccumulatedTimes.getOrDefault(uuid, 0L);
        inCycleAccumulatedTimes.put(uuid, alreadyAccumulatedTime + latestSessionTime);

        latestSessionBeginTimes.remove(uuid);
    }

    // Minütliche Aktualisierung
    private void updateOnlineTimes() {
        long now = System.currentTimeMillis();
        for (UUID uuid : inCycleAccumulatedTimes.keySet()) {
            endSession(uuid);
        }


        // Allerlei Logik mit inCycleAccumulatedTimes

        plugin.saveTimerData();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            beginSession(uuid);
        }
    }


}
