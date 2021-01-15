package me.adarlan.plankton.workflow;

public interface ServiceDependency {

    Service getParentService();

    Service getRequiredService();

    boolean isSatisfied();

    boolean isBlocked();
}
