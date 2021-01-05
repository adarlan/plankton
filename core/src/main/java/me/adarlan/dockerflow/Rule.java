package me.adarlan.dockerflow;

import lombok.ToString;

public interface Rule {

    Job getParentJob();

    String getName();

    Object getValue();

    RuleStatus getStatus();

    boolean updateStatus();

    /*
    @ToString
    public class Data {
        public final String name;
        public final Object value;
        public final String status;

        public Data(Rule rule) {
            this.name = rule.getName();
            this.value = rule.getValue();
            this.status = rule.getStatus().toString().toLowerCase();
        }
    }

    default Data getData() {
        updateStatus();
        return new Data(this);
    }
    */
}