package io.annhilati.playtimelimiter.Rules;

import java.util.List;

public class GroupRule {
    private final String name;
    private final String time;
    private final List<GroupRuleAction> actions;

    public GroupRule(String name, String time, List<GroupRuleAction> actions) {
        this.name = name;
        this.time = time;
        this.actions = actions;
    }

    public String getName() { return name; }
    public String getCronjob() { return time; }
    public List<GroupRuleAction> getActions() { return actions; }

}