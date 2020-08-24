package me.adarlan.dockerflow.rules;

import me.adarlan.dockerflow.Job;

public interface Rule {

    Job getRequiredJob();

    String getName();

    String getValue();

    RuleStatus getRuleStatus();

    void updateStatus();
}