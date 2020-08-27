package me.adarlan.dockerflow;

public interface RuleDependency {

    Job getRequiredJob();
}