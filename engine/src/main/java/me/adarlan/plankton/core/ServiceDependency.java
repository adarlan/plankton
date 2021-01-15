package me.adarlan.plankton.core;

public interface ServiceDependency {

    Service getParentService();

    Service getRequiredService();

    boolean isSatisfied();

    boolean isBlocked();
}
