package me.adarlan.plankton.core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface JobInstance {

    Job getParentJob();

    Integer getNumber();

    String getContainerName();

    Instant getInitialInstant();

    Instant getFinalInstant();

    Duration getDuration();

    Boolean hasStarted();

    Boolean isRunning();

    Boolean hasEnded();

    Integer getExitCode();

    List<String> getLogs();
}
