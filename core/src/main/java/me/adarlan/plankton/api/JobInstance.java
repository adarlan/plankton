package me.adarlan.plankton.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface JobInstance {

    String getParentJob();

    Integer getIndex();

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
