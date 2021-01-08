package me.adarlan.plankton.docker;

public interface RuleWithDependency {

    Job getRequiredJob();
}