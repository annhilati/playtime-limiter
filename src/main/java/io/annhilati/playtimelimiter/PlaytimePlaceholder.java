package io.annhilati.playtimelimiter;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
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

        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();

        return switch (identifier.toLowerCase()) {
            case "time" -> {

                Duration storedTime = Duration.ofSeconds(playtimeTimer.playerData.getInt(uuid + ".time"));
                Duration elapsedTime = Duration.between(playtimeTimer.latestCheckIns.get(uuid), Instant.now());
                if (plugin.getPlaytimeTimer().playerData.get(uuid + ".mode") == "paused") {
                    elapsedTime = Duration.ZERO;
                }

                Duration resultingTime = storedTime.minus(elapsedTime);

                long h = resultingTime.toHoursPart();
                long m = resultingTime.toMinutesPart();
                long s = resultingTime.toSecondsPart();
                                
                yield String.format("%02d:%02d:%02d", h, m, s);

            }
            case "mode" -> {
                String mode = plugin.getPlaytimeTimer().playerData.getString(uuid + ".mode");
                yield mode != null ? mode : "unknown";
            }
            default -> null;
        };
    }
}
