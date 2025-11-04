package io.annhilati.playtimelimiter.Rules;

import io.annhilati.playtimelimiter.PlaytimeLimiter;
import io.annhilati.playtimelimiter.PlaytimeTimer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.*;
import java.time.format.DateTimeFormatter;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class GroupRuleManager {
    
    private final PlaytimeLimiter plugin;
    private final Map<String, List<GroupRule>> groupRules = new HashMap<>();
    
    public GroupRuleManager(PlaytimeLimiter plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();

        parseRulesFromConfig();
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkRules, 20L, 20L * config.getInt("update-cycle"));
    }

    public void parseRulesFromConfig() {

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");

        if (groupsSection == null)
            return;

        for (String groupName : groupsSection.getKeys(false)) {

            ConfigurationSection rulesSection = groupsSection.getConfigurationSection(groupName + ".rules");

            if (rulesSection == null)
                continue;

            List<GroupRule> rules = new ArrayList<>();

            for (String ruleName : rulesSection.getKeys(false)) {

                String timeString = rulesSection.getString(ruleName + ".when.time");
                LocalTime time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("H:mm"));

                ConfigurationSection actionSection = rulesSection.getConfigurationSection(ruleName + ".action");
                List<GroupRuleAction> actions = new ArrayList<>();

                for (String actionType : actionSection.getKeys(false)) {
                    String actionValue = actionSection.getString(actionType);
                    actions.add(new GroupRuleAction(actionType, actionValue));
                }

                rules.add(new GroupRule(ruleName, time, actions));

            }

            groupRules.put(groupName, rules);
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                       Check Rules                                        │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    public void checkRulesFor(UUID uuid) {

        Instant now = Instant.now();

        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();

        Player player = Bukkit.getPlayer(uuid);
        Bukkit.broadcast(Component.text(player.getName()));

        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {
            String groupName = group.getKey();
            Bukkit.broadcast(Component.text(groupName));

            if (player.hasPermission("limiter.group." + groupName)) {

                Bukkit.broadcast(Component.text("Has Perm"));

                for (GroupRule rule : group.getValue()) {
                    Bukkit.broadcast(Component.text(rule.getName()));

                    Instant lastCheckUp = Instant.parse(playerData.getString(uuid + ".last-checkup"));
                    LocalTime ruleTime = rule.getTime();

                    Bukkit.broadcast(Component.text(lastCheckUp.toString()));
                    Bukkit.broadcast(Component.text(now.toString()));
                    long occuranceses = countOccurrences(lastCheckUp, now, ruleTime);

                    Bukkit.broadcast(Component.text(occuranceses));

                    for (int i = 0; i < occuranceses; i++) {
                        applyRule(rule, uuid);
                    }

                }
            }

            playerData.set(uuid + ".last-checkup", now.toString());
            playtimeTimer.savePlayerDataToFile();

        }

    }
    
    public void checkRules() {

        parseRulesFromConfig();
        
        Bukkit.broadcast(Component.text("Rules checking")); // Debug
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            checkRulesFor(uuid);

        }
    }
                
    public int countOccurrences(Instant from, Instant to, LocalTime time) {
        // Grundsätzlich dürfte es keine Dopplung geben, da time schon automatisch 0 Sekunden und Millisekunden hat und dadruch als LocalDateTime auch ziemlich exakt ist
     
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startDateTime = from.atZone(zone);
        ZonedDateTime endDateTime = to.atZone(zone);

        ZonedDateTime onStartDayOccurence = time.atDate(startDateTime.toLocalDate()).atZone(zone); // time ist bereits sekunden clean

        ZonedDateTime firstOccurrence = null;
        if (onStartDayOccurence.isBefore(startDateTime)) {
            firstOccurrence = onStartDayOccurence.plusDays(1);
        } else {
            firstOccurrence = onStartDayOccurence;
        }

        ZonedDateTime onEndDayOccurence = time.atDate(endDateTime.toLocalDate()).atZone(zone); // time ist bereits sekunden clean

        ZonedDateTime lastOccurrence = null;
        if (onEndDayOccurence.isAfter(endDateTime)) {
            lastOccurrence = onEndDayOccurence.minusDays(1);
        } else {
            lastOccurrence = onEndDayOccurence;
        }

        Bukkit.broadcast(Component.text(firstOccurrence.toString()));
        Bukkit.broadcast(Component.text(lastOccurrence.toString()));

        if (lastOccurrence.isBefore(firstOccurrence)) {
            return 0;
        } else if (lastOccurrence.isEqual(firstOccurrence)) {
            return 1;
        } else {
            long daysBetween = Duration.between(firstOccurrence,
                    lastOccurrence).toDays();

            // Wenn negative Zahl, dann keine Vorkommen
            return (int) daysBetween + 1;
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                        Apply Rule                                        │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    private void applyRule(GroupRule rule, UUID uuid) {
        
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
    
        for (GroupRuleAction action : rule.getActions()) {
            String actionType = action.getType();
            String actionValue = action.getValue();
        
            Bukkit.broadcast(Component.text(actionType)); // Debug
    
            switch (actionType) {
        
                case "settimer":
                    Bukkit.broadcast(Component.text("Timer für " + uuid + " ändern auf: " + actionValue));
    
                    if (playtimeTimer.hasSession(uuid)) {
                        playtimeTimer.checkOut(uuid);
                        playerData.set(uuid + ".time", Long.parseLong(actionValue));
                        playtimeTimer.checkIn(uuid);
                    } else {
                        playerData.set(uuid + ".time", Long.parseLong(actionValue));
                    }
        
                case "changetimer":
                    Bukkit.broadcast(Component.text("Timer für " + uuid + "ändern um: " + actionValue));

                    Long oldTime = playerData.getLong(uuid + ".time");
                    
                    if (playtimeTimer.hasSession(uuid)) {
                        playtimeTimer.checkOut(uuid);
                        playerData.set(uuid + ".time", oldTime + Long.parseLong(actionValue));
                        playtimeTimer.checkIn(uuid);
                    } else {
                        playerData.set(uuid + ".time", oldTime + Long.parseLong(actionValue));
                    }

                case "restrict":
                    Bukkit.broadcast(Component.text("Restrict für " + uuid + ": " + actionValue));
                    playerData.set(uuid + ".restrict", bool(actionValue))
                    player kicken, falls er online und restrict true ist
                case "command":
                    Bukkit.broadcast(Component.text("Befehl ausführen: " + actionValue));
                    break;
                default:
                    Bukkit.broadcast(Component.text("Unbekannte Regel: " + actionValue));
        
            }
        
        }
        
    }
}