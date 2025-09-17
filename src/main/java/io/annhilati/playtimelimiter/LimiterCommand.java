package io.annhilati.playtimelimiter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class LimiterCommand implements CommandExecutor {

    private final PlaytimeLimiter plugin;

    public LimiterCommand(PlaytimeLimiter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        commandSender.sendMessage("Angekommen");

        FileConfiguration config = plugin.getConfig();
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
        
        // Get UUID
        UUID uuid = null;
        if (commandSender instanceof Player player) {
            uuid = player.getUniqueId();

        } else {
            commandSender.sendMessage("Kann nicht ausgeführt werden");
            return false;
        }

        // Check Permission
        if (!commandSender.hasPermission("limiter.admin")) {
            commandSender.sendMessage("Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return false;
        }

        playtimeTimer.endTiming(uuid);

        if (args.length >= 2 && args[0].equalsIgnoreCase("mode")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }

            if (args[1].equalsIgnoreCase("ticking")) {

                playerData.set(uuid + ".mode", "ticking");

            } else if (args[1].equalsIgnoreCase("paused")) {

                playerData.set(uuid + ".mode", "paused");
                
                
            } else if (args[1].equalsIgnoreCase("bypass")) {

                playerData.set(uuid + ".mode", "bypass");
                
            } else {

                return false;
                
            }

        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("settimer")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }
        
            playerData.set(uuid + ".time", Long.parseLong(args[1]));

        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("resettimer")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }

            Set<String> groups = new HashSet<>();
            long highest = 0;

            for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
                if (info.getValue() && info.getPermission().startsWith("limiter.group.")) {
                    groups.add(info.getPermission());

                    if (config.getLong("groups." + info.getPermission() + ".start-timer") > highest) {
                        highest = config.getLong("groups." + info.getPermission() + ".start-timer");
                    }
                }
            }

            if (groups.size() == 0) {
                String defaultGroup = config.getString("default-group");
                highest = config.getLong("groups." + defaultGroup + ".start-timer");
            }

            playerData.set(uuid + ".time", highest);

        }

        playtimeTimer.saveTimerData();
        if (Bukkit.getPlayer(uuid) != null) {
            playtimeTimer.beginTiming(uuid);
        }

        return true;
    }
}