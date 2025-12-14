package io.annhilati.playtimelimiter.Rules;

import io.annhilati.playtimelimiter.PlaytimeLimiter;
import io.annhilati.playtimelimiter.PlaytimeTimer;
import io.annhilati.playtimelimiter.Utils.CronCounter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.*;

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

                ConfigurationSection actionSection = rulesSection.getConfigurationSection(ruleName + ".action");
                List<GroupRuleAction> actions = new ArrayList<>();

                for (String actionType : actionSection.getKeys(false)) {
                    String actionValue = actionSection.getString(actionType);
                    actions.add(new GroupRuleAction(actionType, actionValue));
                }

                rules.add(new GroupRule(ruleName, timeString, actions));

            }

            groupRules.put(groupName, rules);
        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                       Check Rules                                        │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    /**
     * UUID muss einem online Player gehören
     */
    public void checkRulesFor(UUID uuid) {

        Instant now = Instant.now();

        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
        
        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {
            String groupName = group.getKey();
            // Bukkit.broadcast(Component.text("Checking for group " + groupName)); // DEBUG

            if (playerData.getString(uuid + ".cached-groups").contains(groupName)) {

                // Bukkit.broadcast(Component.text(player.getName() + " has Perm for " + groupName)); // DEBUG

                for (GroupRule rule : group.getValue()) {
                    // Bukkit.broadcast(Component.text("Checking rule " + rule.getName())); // DEBUG

                    String lastCheckupRaw = playerData.getString(uuid + ".last-checkup");
                    Instant lastCheckUp;
                    if (lastCheckupRaw == null || lastCheckupRaw.isBlank()) { lastCheckUp = Instant.now(); } else { lastCheckUp = Instant.parse(lastCheckupRaw); }

                    String cronjob = rule.getCronjob();

                    // Bukkit.broadcast(Component.text(lastCheckUp.toString())); // DEBUG 
                    // Bukkit.broadcast(Component.text(now.toString())); // DEBUG 
                    long occuranceses = CronCounter.countOccurrences(lastCheckUp, now, cronjob);

                    // Bukkit.broadcast(Component.text("Occurances: " + occuranceses)); // DEBUG 

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

        plugin.getLogger().info("Checking Rules for players");

        parseRulesFromConfig();
        
        // Bukkit.broadcast(Component.text("Rules checking")); // DEBUG
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            checkRulesFor(uuid);

        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                        Apply Rule                                        │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯
    
    private void applyRule(GroupRule rule, UUID uuid) {
        
        PlaytimeTimer playtimeTimer = plugin.getPlaytimeTimer();
        FileConfiguration playerData = plugin.getPlaytimeTimer().playerData;

        Player player = Bukkit.getPlayer(uuid);
    
        for (GroupRuleAction action : rule.getActions()) {
            String actionType = action.getType();
            String actionValue = action.getValue();
        
            // Bukkit.broadcast(Component.text(actionType + actionValue)); // DEBUG
    
            switch (actionType) {
        
                case "settimer":
                    // Bukkit.broadcast(Component.text("Timer für " + uuid + " ändern auf: " + actionValue)); // DEBUG
    
                    if (playtimeTimer.hasSession(uuid)) {
                        playtimeTimer.checkOut(uuid);
                        playerData.set(uuid + ".time", Long.parseLong(actionValue));
                        playtimeTimer.checkIn(uuid);
                    } else {
                        playerData.set(uuid + ".time", Long.parseLong(actionValue));
                    }
                    break;
        
                case "changetimer":
                    // Bukkit.broadcast(Component.text("Timer für " + uuid + "ändern um: " + actionValue)); // DEBUG

                    Long oldTime = playerData.getLong(uuid + ".time");
                    
                    if (playtimeTimer.hasSession(uuid)) {
                        playtimeTimer.checkOut(uuid);
                        playerData.set(uuid + ".time", oldTime + Long.parseLong(actionValue));
                        playtimeTimer.checkIn(uuid);
                    } else {
                        playerData.set(uuid + ".time", oldTime + Long.parseLong(actionValue));
                    }
                    break;

                case "restrict":
                    // Bukkit.broadcast(Component.text("Restrict für " + uuid + ": " + actionValue)); // DEBUG
                    playerData.set(uuid + ".restricted", Boolean.parseBoolean(actionValue));
                    if (player != null && Boolean.parseBoolean(actionValue) == true) {
                        player.kick(Component.text("Restriced"));
                    }
                    break;

                case "execute":
                    // Bukkit.broadcast(Component.text("Befehl ausführen: " + actionValue)); // DEBUG
                    Bukkit.dispatchCommand(player, actionValue);
                    break;

                default:
                    Bukkit.broadcast(Component.text("Unbekannte Aktion: " + actionType));
        
            }
        
        }
        
    }
}