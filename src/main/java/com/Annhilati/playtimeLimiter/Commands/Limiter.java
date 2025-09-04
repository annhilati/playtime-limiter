package com.Annhilati.playtimeLimiter.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Limiter implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        // Überprüfen, ob der Spieler die benötigte Berechtigung hat
        if (commandSender.hasPermission("limiter.admin")) {
            
            if (args.length > 0 && args[0].equalsIgnoreCase("mode")) {

                if (args.length >= 2) {
                    
                    Player player = (Player) commandSender

                    if (args.length >= 3) {

                        Player player = Bukkit.getPlayer(args[2]);

                    }

                    if (args[1].equalsIgnoreCase("ticking")) {

                    } else if (args[1].equalsIgnoreCase("pause")) {
                        
                    } else if (args[1].equalsIgnoreCase("bypass")) {
                        
                    } else {

                        return false;
                    
                    }
                }              
            
            } else {

                return false;
            
            }

        } else {
            // Nachricht, wenn die Berechtigung fehlt
            commandSender.sendMessage("Du hast keine Berechtigung, diesen Befehl auszuführen.");
        }

        return true;
    }
}