package io.annhilati.playtimelimiter.Rules;

public class GroupRuleAction {
    private final String type;
    private final String value;

    public GroupRuleAction(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() { return type; }
    public String getValue() { return value; }
}