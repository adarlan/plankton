package me.adarlan.plankton.core;

public interface ServiceDependency {

    Service getParentService();

    Service getRequiredService();

    ServiceDependencyStatus getStatus();

    Boolean updateStatus();
}
