package me.adarlan.dockerflow;

public interface RuleWithDependency {

    Job getRequiredJob();
}