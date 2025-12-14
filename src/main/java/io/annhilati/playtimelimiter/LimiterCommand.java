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

import net.kyori.adventure.text.Component;

public class LimiterCommand implements CommandExecutor {

    private final PlaytimeLimiter plugin;
    private final FileConfiguration messageConfig;

    public LimiterCommand(PlaytimeLimiter plugin) {
        this.plugin = plugin;
        this.messageConfig = plugin.getMessageConfig();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        commandSender.sendMessage("Angekommen"); // DEBUG

        FileConfiguration config = plugin.getConfig();
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
        
        // Get UUID
        UUID uuid = null;
        if (commandSender instanceof Player player) {
            uuid = player.getUniqueId();

        }
        // else {
        //     commandSender.sendMessage("Kann nicht ausgeführt werden");
        //     return false;
        // }

        // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
        // │                                         Permission                                       │
        // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

        if (!commandSender.hasPermission("limiter.admin")) {
            commandSender.sendMessage(messageConfig.getString("command.missing-permission"));
            return false;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            commandSender.sendMessage(messageConfig.getString("command.reload.success"));
            return true;
        }

        playtimeTimer.checkOut(uuid);

        if (args.length >= 2 && args[0].equalsIgnoreCase("restrict")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }

            if (args[1].equalsIgnoreCase("true")) {

                playerData.set(uuid + ".restricted", true);
                if (Bukkit.getPlayer(uuid) != null) {
                    Bukkit.getPlayer(uuid).kick(Component.text(messageConfig.getString("disconnects.restricted")));
                }

            } else if (args[1].equalsIgnoreCase("false")) {

                playerData.set(uuid + ".restricted", false);

            } else {

            }

            commandSender.sendMessage(messageConfig.getString("command.mode.success"));

        }

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
                
            }

            commandSender.sendMessage(messageConfig.getString("command.mode.success"));

        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("settimer")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }
        
            playerData.set(uuid + ".time", Long.parseLong(args[1]));

            commandSender.sendMessage(messageConfig.getString("command.settimer.success"));

        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("resettimer")) {

            if (args.length >= 3) {
                uuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            }

            Set<String> groups = new HashSet<>();
            long highest = 0;

            for (PermissionAttachmentInfo info : Bukkit.getPlayer(uuid).getEffectivePermissions()) {
                if (info.getValue() && info.getPermission().startsWith("limiter.group.")) {
                    groups.add(info.getPermission());

                    if (config.getLong("groups." + info.getPermission() + ".start-timer") > highest) {
                        highest = config.getLong("groups." + info.getPermission() + ".start-timer");
                    }
                }
            }

            if (groups.isEmpty()) {
                String defaultGroup = config.getString("default-group");
                highest = config.getLong("groups." + defaultGroup + ".start-timer");
            }

            playerData.set(uuid + ".time", highest);

            commandSender.sendMessage(messageConfig.getString("command.resettimer.success"));

        }

        playtimeTimer.savePlayerDataToFile();
        if (Bukkit.getPlayer(uuid) != null) {
            playtimeTimer.checkIn(uuid);
        }

        return true; // obligatorisch
    }
}