package me.adarlan.plankton.docker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import me.adarlan.plankton.core.Pipeline;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceDependencyStatus;
import me.adarlan.plankton.core.ServiceInstance;
import me.adarlan.plankton.core.ServiceStatus;
import me.adarlan.plankton.logging.Colors;

@ToString(of = "name")
@EqualsAndHashCode(of = "name")
public class ServiceImplementation implements Service {

    final PipelineImplementation pipeline;
    final String name;
    final DockerCompose dockerCompose;

    String expression;
    Boolean expressionResult;

    final Set<ServiceDependency> dependencies = new HashSet<>();

    Integer scale;
    final List<ServiceInstanceImplementation> instances = new ArrayList<>();

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    Duration timeoutLimit;

    ServiceStatus status;

    boolean needToBuild;
    private Thread buildOrPullImage = null;
    private boolean imageBuiltOrPulled = false;
    private boolean startedInstances = false;
    private boolean ended = false;

    private final List<String> logs = new ArrayList<>();
    String color;
    // String colorizedName;
    String infoPrefix;
    String logPrefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Marker LOG_MARKER = MarkerFactory.getMarker("LOG");
    private static final String LOG_PLACEHOLDER = "{}{}";
    private static final String INFO_PLACEHOLDER = "{}" + Colors.BRIGHT_WHITE + "{}" + Colors.ANSI_RESET;

    ServiceImplementation(PipelineImplementation pipeline, String name) {
        this.pipeline = pipeline;
        this.dockerCompose = pipeline.dockerCompose;
        this.name = name;
    }

    void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.info(LOG_MARKER, LOG_PLACEHOLDER, logPrefix, message);
    }

    void refresh() {
        synchronized (this) {
            if (!ended) {
                if (status == ServiceStatus.WAITING) {
                    checkDependenciesAndSetRunningOrBlocked();
                }
                if (status == ServiceStatus.RUNNING) {
                    if (startedInstances) {
                        checkInstancesAndSetSuccessOrFailure();
                    } else {
                        if (buildOrPullImage == null) {
                            buildOrPullImage = new BuildOrPullImage();
                            buildOrPullImage.start();
                        } else if (imageBuiltOrPulled) {
                            createContainers();
                            startInstances();
                        } else if (buildOrPullImage.isInterrupted()) {
                            logger.error(INFO_PLACEHOLDER, infoPrefix, "Interrupted when building/pulling image");
                            setStatus(ServiceStatus.FAILURE);
                        }
                    }
                }
            }
        }
    }

    private void checkDependenciesAndSetRunningOrBlocked() {
        boolean passed = true;
        boolean blocked = false;
        for (final ServiceDependency dependency : dependencies) {
            if (dependency.updateStatus()) {
                logDependencyInfo(dependency);
            }
            if (!dependency.getStatus().equals(ServiceDependencyStatus.PASSED))
                passed = false;
            if (dependency.getStatus().equals(ServiceDependencyStatus.BLOCKED))
                blocked = true;
        }
        if (passed) {
            setStatus(ServiceStatus.RUNNING);
        } else if (blocked) {
            setStatus(ServiceStatus.BLOCKED);
        }
    }

    private class BuildOrPullImage extends Thread {

        private BuildOrPullImage() {
            super();
        }

        @Override
        public void run() {
            if (needToBuild) {
                if (!dockerCompose.buildImage(ServiceImplementation.this)) {
                    setStatus(ServiceStatus.FAILURE);
                }
            } else {
                if (!dockerCompose.pullImage(ServiceImplementation.this)) {
                    setStatus(ServiceStatus.FAILURE);
                }
            }
            imageBuiltOrPulled = true;
        }
    }

    private void createContainers() {
        if (!dockerCompose.createContainers(this)) {
            setStatus(ServiceStatus.FAILURE);
        }
    }

    private void startInstances() {
        instances.forEach(ServiceInstanceImplementation::start);
        startedInstances = true;
    }

    private void checkInstancesAndSetSuccessOrFailure() {
        boolean success = true;
        boolean failure = false;
        for (ServiceInstanceImplementation instance : instances) {
            instance.refresh();
            if (instance.hasEnded()) {
                Integer exitCode = instance.getExitCode();
                if (exitCode == null || !exitCode.equals(0)) {
                    failure = true;
                    success = false;
                }
            } else {
                success = false;
            }
        }
        if (success) {
            setStatus(ServiceStatus.SUCCESS);
        } else if (failure) {
            setStatus(ServiceStatus.FAILURE);
        } else {
            checkTimeout();
        }
    }

    private void checkTimeout() {
        Duration d = getDuration();
        if (d.compareTo(timeoutLimit) > 0) {
            logger.error(INFO_PLACEHOLDER, infoPrefix, "Time limit has been reached");
            instances.forEach(ServiceInstanceImplementation::stop);
        }
    }

    private void setStatus(ServiceStatus status) {
        this.status = status;
        switch (status) {
            case DISABLED:
                ended = true;
                logger.info(INFO_PLACEHOLDER, infoPrefix, "Disabled");
                break;
            case WAITING:
                dependencies.forEach(this::logDependencyInfo);
                break;
            case BLOCKED:
                ended = true;
                logger.info(INFO_PLACEHOLDER, infoPrefix, "Blocked");
                break;
            case RUNNING:
                initialInstant = Instant.now();
                logger.info(INFO_PLACEHOLDER, infoPrefix, "Running");
                break;
            case FAILURE:
                logger.info(INFO_PLACEHOLDER, infoPrefix, "Failed");
                break;
            case SUCCESS:
                ended = true;
                finalInstant = Instant.now();
                duration = Duration.between(initialInstant, finalInstant);
                logger.info(INFO_PLACEHOLDER, infoPrefix, "Succeeded");
                break;
        }
    }

    private void logDependencyInfo(ServiceDependency dependency) {
        logger.info(INFO_PLACEHOLDER, infoPrefix, dependency);
    }

    public Duration getDuration() {
        if (duration == null) {
            if (initialInstant == null) {
                return Duration.ZERO;
            } else {
                return Duration.between(initialInstant, Instant.now());
            }
        } else {
            return duration;
        }
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public List<ServiceInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    @Override
    public Set<ServiceDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    @Override
    public Boolean hasEnded() {
        return ended;
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ServiceStatus getStatus() {
        return status;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public Boolean getExpressionResult() {
        return expressionResult;
    }

    @Override
    public Integer getScale() {
        return scale;
    }

    @Override
    public Instant getInitialInstant() {
        return initialInstant;
    }

    @Override
    public Duration getTimeoutLimit() {
        return timeoutLimit;
    }

    @Override
    public Instant getFinalInstant() {
        return finalInstant;
    }
}