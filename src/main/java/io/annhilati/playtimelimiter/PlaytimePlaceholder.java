package io.annhilati.playtimelimiter;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlaytimePlaceholder extends PlaceholderExpansion {

    private final PlaytimeLimiter plugin;

    public PlaytimePlaceholder(PlaytimeLimiter plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playtimelimiter"; // Dein Präfix für %playtimelimiter_<name>%
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    // Hier werden Platzhalter verarbeitet
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null)
            return "";
        UUID uuid = player.getUniqueId();

        return switch (identifier.toLowerCase()) {
            case "time" -> {
                int time = plugin.getPlaytimeTimer().timerData.getInt(uuid + ".time");
                yield String.valueOf(time);
            }
            case "mode" -> {
                String mode = plugin.getPlaytimeTimer().timerData.getString(uuid + ".mode");
                yield mode != null ? mode : "unknown";
            }
            default -> null;
        };
    }
}
