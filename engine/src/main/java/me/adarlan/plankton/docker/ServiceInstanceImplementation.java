package me.adarlan.plankton.docker;

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
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceInstance;

@ToString(of = "containerName")
@EqualsAndHashCode(of = "containerName")
public class ServiceInstanceImplementation implements ServiceInstance {

    final ServiceImplementation parentService;
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

    ServiceInstanceImplementation(ServiceImplementation parentService, int number) {
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

    @Override
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

    @Override
    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean hasEnded() {
        return ended;
    }

    @Override
    public Service getParentService() {
        return parentService;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    @Override
    public String getContainerName() {
        return containerName;
    }

    @Override
    public Instant getInitialInstant() {
        return initialInstant;
    }

    @Override
    public Instant getFinalInstant() {
        return finalInstant;
    }

    @Override
    public Integer getExitCode() {
        return exitCode;
    }
}
