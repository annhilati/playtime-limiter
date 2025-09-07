package io.annhilati.playtimelimiter;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LimiterCommand implements CommandExecutor {

    private final PlaytimeLimiter plugin;

    public LimiterCommand(PlaytimeLimiter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        commandSender.sendMessage("Angekommen");

        FileConfiguration timerData = plugin.getTimerData();
        
        // Get UUID
        UUID uuid = null;
        if (args.length == 3) {
            uuid = Bukkit.getPlayer(args[2]).getUniqueId();
        } else if (commandSender instanceof Player player) {
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

        if (args.length > 0 && args[0].equalsIgnoreCase("mode")) {

            if (args[1].equalsIgnoreCase("ticking")) {

                timerData.set(uuid + ".mode", "ticking");

                if (plugin.getPlayertimeTimer().latestSessionBegins.get(uuid) == null) {
                    plugin.getPlayertimeTimer().beginSession(uuid);
                }

            } else if (args[1].equalsIgnoreCase("paused")) {

                timerData.set(uuid + ".mode", "paused");
                
                if (plugin.getPlayertimeTimer().latestSessionBegins.get(uuid) != null) {
                    plugin.getPlayertimeTimer().endSession(uuid);
                }
                
            } else if (args[1].equalsIgnoreCase("bypass")) {

                timerData.set(uuid + ".mode", "bypass");
                
                if (plugin.getPlayertimeTimer().latestSessionBegins.get(uuid) == null) {
                    plugin.getPlayertimeTimer().beginSession(uuid);
                }
                
            } else {

                return false;
                
            }

        }

        if (args.length > 0 && args[0].equalsIgnoreCase("settimer")) {
        
            timerData.set(uuid + ".time", args[1]);

        }

        return true;
    }
}