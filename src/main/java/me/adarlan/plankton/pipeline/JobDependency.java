package me.adarlan.plankton.pipeline;

public interface JobDependency {

    Job getParentJob();

    Job getRequiredJob();

    boolean isSatisfied();

    boolean isBlocked();
}
