package me.adarlan.plankton.core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface Service {

    Pipeline getPipeline();

    String getName();

    Set<ServiceDependency> getDependencies();

    ServiceStatus getStatus();

    String getExpression();

    Boolean getExpressionResult();

    Integer getScale();

    List<ServiceInstance> getInstances();

    List<String> getLogs();

    Instant getInitialInstant();

    Duration getDuration();

    Duration getTimeoutLimit();

    Instant getFinalInstant();
}
