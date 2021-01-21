package me.adarlan.plankton.pipeline;

public interface JobDependency {

    Job job();

    boolean isSatisfied();

    boolean isBlocked();
}
