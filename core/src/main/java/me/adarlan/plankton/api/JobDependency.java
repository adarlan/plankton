package me.adarlan.plankton.api;

public interface JobDependency {

    Job getParentJob();

    Job getRequiredJob();

    JobDependencyStatus getStatus();

    Boolean updateStatus();
}
