package io.annhilati.playtimelimiter.Rules;

import java.time.LocalTime;
import java.util.List;

public class GroupRule {
    private final String name;
    private final LocalTime time;
    private final List<GroupRuleAction> actions;

    public GroupRule(String name, LocalTime time, List<GroupRuleAction> actions) {
        this.name = name;
        this.time = time;
        this.actions = actions;
    }

    public String getName() { return name; }

    public LocalTime getTime() { return time; }

    public List<GroupRuleAction> getActions() { return actions; }


}