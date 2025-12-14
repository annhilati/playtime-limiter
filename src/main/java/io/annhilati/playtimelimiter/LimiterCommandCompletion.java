package io.annhilati.playtimelimiter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LimiterCommandCompletion implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
                
        List<String> completions = new ArrayList<>();

        // Subcommands
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("settimer", "mode", "resettimer", "reload");

            for (String s : subcommands) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }

        // Argumente für mode
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return Arrays.asList("ticking", "paused", "bypass");
        }

        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }

        return completions;
    }
}