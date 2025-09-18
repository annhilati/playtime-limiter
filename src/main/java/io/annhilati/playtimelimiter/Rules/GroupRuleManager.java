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

    
    public void checkRules() {

        parseRulesFromConfig();

        Instant now = Instant.now();
        LocalTime nowClock = LocalTime.now().withSecond(0).withNano(0);
        
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        
        Bukkit.broadcast(Component.text("Rules checking")); // Debug
        
        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {
            String groupName = group.getKey();
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                if (player.hasPermission("limiter.group." + groupName)); {

                    for (GroupRule rule : group.getValue()) {

                        Instant lastCheckUp = Instant.parse(playerData.getString(uuid + "last-checkup"));
                        LocalTime ruleTime = rule.getTime();

                        int occuranceses = countOccurrences(lastCheckUp, now, ruleTime, ZoneId.systemDefault());
                        
                        for (int i = 0; i < occuranceses; i++) {
                            executeRule(rule, uuid);
                        }

                    }
                }

                playerData.set(uuid + ".last-checkup", now.toString());

            }
        }
    }
            
    public int countOccurrences(Instant from, Instant to, LocalTime time, ZoneId zone) {
        
        // 1. Start und Ende in LocalDateTime umwandeln
        ZonedDateTime startDateTime = from.atZone(zone);
        ZonedDateTime endDateTime = to.atZone(zone);
    
        // 2. Erstes Vorkommen nach Start
        ZonedDateTime firstOccurrence = time.atDate(startDateTime.toLocalDate()).atZone(zone);
    
        if (firstOccurrence.isBefore(startDateTime)) {
            // Wenn die Zeit am Starttag schon vorbei ist, auf nächsten Tag
            firstOccurrence = firstOccurrence.plusDays(1);
        }
    
        // 3. Letztes Vorkommen vor Ende
        ZonedDateTime lastOccurrence = time.atDate(endDateTime.toLocalDate()).atZone(zone);
    
        if (lastOccurrence.isAfter(endDateTime)) {
            // Wenn die Zeit am Endtag noch nicht erreicht ist, einen Tag zurück
            lastOccurrence = lastOccurrence.minusDays(1);
        }
    
        // 4. Anzahl der Tage zwischen erstem und letztem Vorkommen
        long daysBetween = Duration.between(firstOccurrence.toLocalDate().atStartOfDay(zone),
                lastOccurrence.toLocalDate().atStartOfDay(zone)).toDays();
    
        // Wenn negative Zahl, dann keine Vorkommen
        return daysBetween < 0 ? 0 : (int) daysBetween + 1;
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                        Execute Rule                                      │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    private void executeRule(GroupRule rule, UUID uuid) {
        
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
    
        for (GroupRuleAction action : rule.getActions()) {
            String actionType = action.getType();
            String actionValue = action.getValue();
        
            Bukkit.broadcast(Component.text(actionType)); // Debug
    
            switch (actionType) {
        
                case "settimer":
                    plugin.getLogger().info("Timer für " + uuid + " ändern auf: " + actionValue);
    
                    playtimeTimer.checkOut(uuid);
                    playerData.set(uuid + ".time", playerData);
                    playtimeTimer.checkIn(uuid);
        
                    break;
                case "restrict":
                    plugin.getLogger().info("Restrict für " + uuid + ": " + actionValue);
                    break;
                case "changetimer":
                    plugin.getLogger().info("Timer für " + uuid + "ändern um: " + actionValue);
                    break;
                case "command":
                    plugin.getLogger().info("Befehl ausführen: " + actionValue);
                    break;
                default:
                    plugin.getLogger().warning("Unbekannte Regel: " + actionValue);
        
            }
        
        }
        
    }
}