package io.annhilati.playtimelimiter.Rules;

import io.annhilati.playtimelimiter.PlaytimeLimiter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class GroupRuleManager {
    
    private final PlaytimeLimiter plugin;
    private final Map<String, List<GroupRule>> groupRules = new HashMap<>();
    
    public GroupRuleManager(PlaytimeLimiter plugin) {
        this.plugin = plugin;
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
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        Bukkit.broadcast(Component.text("Rules checking")); // Debug

        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {
            
            String groupName = group.getKey();
            Bukkit.broadcast(Component.text(groupName + ": " + group.getValue().size())); // Debug
            
            for (GroupRule rule : group.getValue()) {
                if (rule.getTime().equals(now)) {
                    executeRule(groupName, rule);
                }
            }

        }
    }

    // ╭──────────────────────────────────────────────────────────────────────────────────────────╮
    // │                                        Execute Rule                                      │
    // ╰──────────────────────────────────────────────────────────────────────────────────────────╯

    private void executeRule(String group, GroupRule rule) {

        String permission = "limiter.group." + group;

        for (GroupRuleAction action : rule.getActions()) {

            Bukkit.broadcast(Component.text(action.getType())); // Debug

            switch (action.getType()) {

                case "settimer":
                    plugin.getLogger().info("Timer für Gruppe " + group + " ändern auf: " + action.getValue());
                    break;
                case "restrict":
                    plugin.getLogger().info("Restrict für Gruppe " + group + ": " + action.getValue());
                    break;
                case "changetimer":
                    plugin.getLogger().info("Timer für Gruppe " + group + "ändern um: " + action.getValue());
                    break;
                case "command":
                    plugin.getLogger().info("Befehl ausführen: " + action.getValue());
                    break;
                default:
                    plugin.getLogger().warning("Unbekannte Regel: " + action.getType());
            
            }

        }
        
    }
}