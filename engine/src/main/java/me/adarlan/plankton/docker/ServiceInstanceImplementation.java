package me.adarlan.plankton.docker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.adarlan.plankton.core.ServiceInstance;
import me.adarlan.plankton.logging.Logger;

@ToString(of = "containerName")
@EqualsAndHashCode(of = "containerName")
public class ServiceInstanceImplementation implements ServiceInstance {

    @Getter
    private final ServiceImplementation parentService;

    @Getter
    private final Integer number;

    @Getter
    private final String containerName;

    private final List<String> logs = new ArrayList<>();

    private boolean started = false;
    private boolean running = false;
    private boolean ended = false;
    private boolean stop = false;

    @Getter
    private Instant initialInstant = null;

    @Getter
    private Instant finalInstant = null;

    @Getter
    private Duration duration = Duration.ZERO;

    @Getter
    private Integer exitCode = null;

    private Thread runContainerThread = null;

    private final DockerCompose dockerCompose;

    private final Logger logger = Logger.getLogger();

    ServiceInstanceImplementation(ServiceImplementation parentService, int number) {
        this.parentService = parentService;
        this.number = number;
        this.containerName = parentService.getPipeline().getId() + "_" + parentService.getName() + "_" + number;
        this.dockerCompose = parentService.getPipeline().dockerCompose;
    }

    void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.log(this, () -> message);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
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
}
