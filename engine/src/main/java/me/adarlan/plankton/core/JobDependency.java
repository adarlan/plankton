package me.adarlan.plankton.core;

public interface JobDependency {

    Job getParentJob();

    Job getRequiredJob();

    JobDependencyStatus getStatus();

    Boolean updateStatus();
}
