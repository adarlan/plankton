package me.adarlan.plankton.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface Job {

    Pipeline getPipeline();

    String getName();

    Set<JobDependency> getDependencies();

    JobStatus getStatus();

    String getExpression();

    Boolean getExpressionResult();

    Integer getScale();

    List<JobInstance> getInstances();

    List<String> getLogs();

    Instant getInitialInstant();

    Duration getDuration();

    Duration getTimeoutLimit();

    Boolean hasEnded();

    Instant getFinalInstant();
}
