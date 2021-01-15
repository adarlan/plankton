package me.adarlan.plankton.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ServiceInstance {

    Service getParentService();

    Integer getNumber();

    String getContainerName();

    Instant getInitialInstant();

    Instant getFinalInstant();

    Duration getDuration();

    boolean hasStarted();

    boolean isRunning();

    boolean hasEnded();

    Integer getExitCode();

    List<String> getLogs();
}
