package io.annhilati.playtimelimiter.Rules;

import io.annhilati.playtimelimiter.PlaytimeLimiter;
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
        LocalTime clockNow = LocalTime.now().withSecond(0).withNano(0);

        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;

        Bukkit.broadcast(Component.text("Rules checking")); // Debug

        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                for (GroupRule rule : group.getValue()) {
                    LocalTime ruleTime = rule.getTime();
                    Instant lastChekUp = Instant.parse(playerData.getString(uuid + "last-checkup"));
                    
                    // 1. Start und Ende in LocalDateTime umwandeln
                    ZonedDateTime startDateTime = lastChekUp.atZone(ZoneId.systemDefault());
                    ZonedDateTime endDateTime = now.atZone(ZoneId.systemDefault());

                    // 2. Erstes Vorkommen nach Start
                    LocalDate firstDate = startDateTime.toLocalDate();
                    ZonedDateTime firstOccurrence = clockNow.atDate(firstDate).atZone(ZoneId.systemDefault());

                    if (firstOccurrence.isBefore(startDateTime)) {
                        // Wenn die Zeit am Starttag schon vorbei ist, auf nächsten Tag
                        firstOccurrence = firstOccurrence.plusDays(1);
                    }

                    // 3. Letztes Vorkommen vor Ende
                    LocalDate lastDate = endDateTime.toLocalDate();
                    ZonedDateTime lastOccurrence = clockNow.atDate(lastDate).atZone(ZoneId.systemDefault());

                    if (lastOccurrence.isAfter(endDateTime)) {
                        // Wenn die Zeit am Endtag noch nicht erreicht ist, einen Tag zurück
                        lastOccurrence = lastOccurrence.minusDays(1);
                    }

                    // 4. Anzahl der Tage zwischen erstem und letztem Vorkommen
                    long daysBetween = Duration.between(firstOccurrence.toLocalDate().atStartOfDay(ZoneId.systemDefault()),
                                                        lastOccurrence.toLocalDate().atStartOfDay(ZoneId.systemDefault())).toDays();

                    // Wenn negative Zahl, dann keine Vorkommen
                    int occurences = daysBetween < 0 ? 0 : (int) daysBetween + 1;

                }

            }
            
            // String groupName = group.getKey();
            // Bukkit.broadcast(Component.text(groupName + ": " + group.getValue().size())); // Debug
            
            // for (GroupRule rule : group.getValue()) {
            //     if (rule.getTime().equals(now)) {
            //         executeRule(groupName, rule);
            //     }
            // }
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                        Execute Rule                                      │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    // private void executeRule(String group, GroupRule rule) {

    //     String permission = "limiter.group." + group;
    //     FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;

    //     for (GroupRuleAction action : rule.getActions()) {

    //         Bukkit.broadcast(Component.text(action.getType())); // Debug

    //         switch (action.getType()) {

    //             case "settimer":
    //                 plugin.getLogger().info("Timer für Gruppe " + group + " ändern auf: " + action.getValue());
                    
    //                 // for (String str : playerData.getKeys(false)) {
    //                 //     UUID uuid = UUID.fromString(str);
    //                 //     if Bukkit.getOfflinePlayer(uuid).has
    //                 // }
                    
    //                 break;
    //             case "restrict":
    //                 plugin.getLogger().info("Restrict für Gruppe " + group + ": " + action.getValue());
    //                 break;
    //             case "changetimer":
    //                 plugin.getLogger().info("Timer für Gruppe " + group + "ändern um: " + action.getValue());
    //                 break;
    //             case "command":
    //                 plugin.getLogger().info("Befehl ausführen: " + action.getValue());
    //                 break;
    //             default:
    //                 plugin.getLogger().warning("Unbekannte Regel: " + action.getType());
            
    //         }

    //     }
        
    // }
}

/* 
@ tastTimer
for group in groups:
    for player in onlinePlayers:
        for rule in rules:
            if player.lastCheckUp < rule.time < now:
                rule.execute(player)
 */