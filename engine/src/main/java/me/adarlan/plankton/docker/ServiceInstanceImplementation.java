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

import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceInstance;

@ToString(of = "containerName")
@EqualsAndHashCode(of = "containerName")
public class ServiceInstanceImplementation implements ServiceInstance {

    private final ServiceImplementation parentService;
    final Integer number;
    private final String containerName;

    private final List<String> logs = new ArrayList<>();

    private boolean started = false;
    private boolean running = false;
    private boolean ended = false;
    private boolean stop = false;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = Duration.ZERO;

    private Integer exitCode = null;

    private Thread runContainerThread = null;

    private final DockerCompose dockerCompose;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Marker LOG_MARKER = MarkerFactory.getMarker("LOG");
    private static final String LOG_PLACEHOLDER = "{} -> {}";

    String name;
    String colorizedName;

    ServiceInstanceImplementation(ServiceImplementation parentService, int number) {
        this.parentService = parentService;
        this.number = number;
        this.containerName = parentService.pipeline.getId() + "_" + parentService.getName() + "_" + number;
        this.dockerCompose = parentService.pipeline.dockerCompose;
    }

    void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.info(LOG_MARKER, LOG_PLACEHOLDER, colorizedName, message);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    void start() {
        runContainerThread = new Thread(() -> dockerCompose.runContainer(this));
        runContainerThread.start();
        this.started = true;
    }

    void refresh() {
        synchronized (this) {
            if (started && !ended) {
                ContainerState containerState = dockerCompose.getContainerState(this);
                refresh(containerState);
                refreshDuration();
            }
        }
    }

    private void refresh(ContainerState containerState) {
        if (containerState.status.equals("running")) {
            if (!running) {
                running = true;
                initialInstant = parseInstant(containerState.startedAt);
            }
            if (stop) {
                dockerCompose.stopContainer(this);
            }
        } else if (containerState.status.equals("exited")) {
            running = false;
            ended = true;
            if (initialInstant == null) {
                initialInstant = parseInstant(containerState.startedAt);
            }
            finalInstant = parseInstant(containerState.finishedAt);
            exitCode = containerState.exitCode;
        } else if (!containerState.error.isBlank()) {
            running = false;
            ended = true;
            if (initialInstant == null) {
                initialInstant = parseInstant(containerState.startedAt);
            }
            finalInstant = parseInstant(containerState.finishedAt);
            if (containerState.exitCode != 0) {
                exitCode = containerState.exitCode;
            }
        } else if (containerState.status.equals("created")) {
            /* ok */
        } else {
            throw new PlanktonDockerException("Unexpected container state: " + containerState);
            // TODO stop thread? failed?
        }
    }

    private Instant parseInstant(String text) {
        if (text.equals("0001-01-01T00:00:00Z"))
            return null;
        else
            return Instant.parse(text);
    }

    private void refreshDuration() {
        if (running) {
            duration = Duration.between(initialInstant, Instant.now());
        } else if (ended && initialInstant != null && finalInstant != null) {
            duration = Duration.between(initialInstant, finalInstant);
        }
    }

    void stop() {
        stop = true;
        dockerCompose.stopContainer(this);
    }

    @Override
    public Boolean hasStarted() {
        return started;
    }

    @Override
    public Boolean isRunning() {
        return running;
    }

    @Override
    public Boolean hasEnded() {
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
    public Duration getDuration() {
        return duration;
    }

    @Override
    public Integer getExitCode() {
        return exitCode;
    }
}
