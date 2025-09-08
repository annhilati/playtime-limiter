package io.annhilati.playtimelimiter.Rules;

import io.annhilati.playtimelimiter.PlaytimeLimiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

public class GroupRuleManager {
    
    private final PlaytimeLimiter plugin;
    private final Map<String, List<GroupRule>> groupRules = new HashMap<>();
    
    public GroupRuleManager(PlaytimeLimiter plugin) {
        this.plugin = plugin;
    }

    public void loadRules() {

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

    public void checkRules() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        for (Map.Entry<String, List<GroupRule>> group : groupRules.entrySet()) {
            
            String groupName = group.getKey();
            
            for (GroupRule rule : group.getValue()) {
                if (rule.getTime().equals(now)) {
                    executeRule(groupName, rule);
                }
            }

        }
    }

    private void executeRule(String group, GroupRule rule) {

        for (GroupRuleAction action : rule.getActions()) {

            switch (action.getType()) {

                case "change-timer":
                    plugin.getLogger().info("Timer für Gruppe " + group + " ändern: " + action.getValue());
                    break;
                case "restrict":
                    plugin.getLogger().info("Restrict für Gruppe " + group + ": " + action.getValue());
                    break;
                default:
                    plugin.getLogger().warning("Unbekannte Regel: " + action.getType());
            
            }

        }
        
    }
}