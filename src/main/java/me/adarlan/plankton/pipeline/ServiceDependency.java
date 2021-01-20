package me.adarlan.plankton.pipeline;

public interface ServiceDependency {

    Service getParentService();

    Service getRequiredService();

    boolean isSatisfied();

    boolean isBlocked();
}
