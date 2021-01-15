package me.adarlan.plankton.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ContainerState;

@ToString(of = "containerName")
@EqualsAndHashCode(of = "containerName")
public class ServiceInstance {

    final Service parentService;
    final Integer number;
    private final String containerName;

    private final List<String> logs = new ArrayList<>();

    private boolean started = false;
    private boolean running = false;
    private boolean ended = false;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;

    private Integer exitCode = null;

    private Thread runContainerThread = null;

    private final Compose compose;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Marker LOG_MARKER = MarkerFactory.getMarker("LOG");
    private static final String LOG_PLACEHOLDER = "{}{}";

    String name;
    String logPrefix;

    ServiceInstance(Service parentService, int number) {
        this.parentService = parentService;
        this.number = number;
        this.containerName = parentService.pipeline.getId() + "_" + parentService.getName() + "_" + number;
        this.compose = parentService.pipeline.compose;
    }

    private void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.info(LOG_MARKER, LOG_PLACEHOLDER, logPrefix, message);
    }

    void start() {
        runContainerThread = new Thread(() -> compose.runContainer(containerName, this::log, this::log));
        runContainerThread.start();
        this.started = true;
    }

    void stop() {
        synchronized (this) {
            compose.stopContainer(containerName);
        }
    }

    void refresh() {
        synchronized (this) {
            if (started && !ended) {
                ContainerState containerState = compose.getContainerState(containerName);
                refresh(containerState);
            }
        }
    }

    private void refresh(ContainerState containerState) {
        if (containerState.running()) {
            if (!running) {
                running = true;
                initialInstant = containerState.initialInstant();
            }
        } else if (containerState.exited()) {
            running = false;
            ended = true;
            if (initialInstant == null) {
                initialInstant = containerState.initialInstant();
            }
            finalInstant = containerState.finalInstant();
            exitCode = containerState.exitCode();
        }
    }

    public Duration getDuration() {
        if (duration == null) {
            if (initialInstant != null && finalInstant != null) {
                duration = Duration.between(initialInstant, finalInstant);
                return duration;
            } else if (initialInstant != null) {
                return Duration.between(initialInstant, Instant.now());
            } else {
                return Duration.ZERO;
            }
        } else {
            return duration;
        }
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public boolean hasStarted() {
        return started;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean hasEnded() {
        return ended;
    }

    public Service getParentService() {
        return parentService;
    }

    public Integer getNumber() {
        return number;
    }

    public String getContainerName() {
        return containerName;
    }

    public Instant getInitialInstant() {
        return initialInstant;
    }

    public Instant getFinalInstant() {
        return finalInstant;
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
