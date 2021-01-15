package me.adarlan.plankton.workflow;

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
import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.logging.Colors;

@ToString(of = "name")
@EqualsAndHashCode(of = "name")
public class Service {

    final Pipeline pipeline;
    final String name;
    ServiceStatus status;

    String expression;
    Boolean expressionResult;

    final Set<ServiceDependency> dependencies = new HashSet<>();

    Integer scale;
    final List<ServiceInstance> instances = new ArrayList<>();

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;

    Duration timeoutLimit;

    boolean needToBuild;
    private Thread buildImage = null;
    private Thread pullImage = null;
    private boolean imageBuilt = false;
    private boolean imagePulled = false;

    private boolean startedInstances = false;

    String color;
    String infoPrefix;
    String logPrefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String INFO_PLACEHOLDER = "{}" + Colors.BRIGHT_WHITE + "{}" + Colors.ANSI_RESET;

    private static final Marker LOG_MARKER = MarkerFactory.getMarker("LOG");
    private static final String LOG_PLACEHOLDER = "{}{}";

    final Compose compose;

    private final List<String> logs = new ArrayList<>();

    Service(Pipeline pipeline, String name) {
        this.pipeline = pipeline;
        this.compose = pipeline.compose;
        this.name = name;
    }

    private void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.info(LOG_MARKER, LOG_PLACEHOLDER, logPrefix, message);
    }

    void refresh() {
        synchronized (this) {
            if (status == ServiceStatus.WAITING) {
                checkDependenciesAndSetRunningOrBlocked();
            }
            if (status == ServiceStatus.RUNNING) {
                if (!startedInstances) {
                    if (needToBuild) {
                        buildImageAndCreateContainersAndStartInstances();
                    } else {
                        pullImageAndCreateContainersAndStartInstances();
                    }
                }
                if (startedInstances) {
                    checkInstancesAndSetSuccessOrFailure();
                }
            }
        }
    }

    private void checkDependenciesAndSetRunningOrBlocked() {
        boolean passed = true;
        boolean blocked = false;
        for (final ServiceDependency dependency : dependencies) {
            if (!dependency.isSatisfied()) {
                passed = false;
            }
            if (dependency.isBlocked()) {
                blocked = true;
            }
        }
        if (passed) {
            setRunning();
        } else if (blocked) {
            setBlocked();
        }
    }

    private void buildImageAndCreateContainersAndStartInstances() {
        if (buildImage == null) {
            buildImage = new Thread(() -> {
                Service service = Service.this;
                if (compose.buildImage(name, service::log, service::log)) {
                    imageBuilt = true;
                } else {
                    setFailure("Failed when building image");
                }
            });
            buildImage.start();
        } else if (imageBuilt) {
            createContainers();
            startInstances();
        } else if (buildImage.isInterrupted()) {
            setFailure("Interrupted when building image");
        }
    }

    private void pullImageAndCreateContainersAndStartInstances() {
        if (pullImage == null) {
            pullImage = new Thread(() -> {
                Service service = Service.this;
                if (compose.pullImage(name, service::log, service::log)) {
                    imagePulled = true;
                } else {
                    setFailure("Failed when pulling image");
                }
            });
            pullImage.start();
        } else if (imagePulled) {
            createContainers();
            startInstances();
        } else if (pullImage.isInterrupted()) {
            setFailure("Interrupted when pulling image");
        }
    }

    private void createContainers() {
        if (!compose.createContainers(name, scale, this::log, this::log)) {
            setFailure("Failed when creating " + (scale > 1 ? "containers" : "container"));
        }
    }

    private void startInstances() {
        instances.forEach(ServiceInstance::start);
        startedInstances = true;
    }

    private void checkInstancesAndSetSuccessOrFailure() {
        boolean success = true;
        boolean failure = false;
        for (ServiceInstance instance : instances) {
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
            setSuccess();
        } else if (failure) {
            setFailure("Failed");
            // TODO log exit code information
        } else {
            checkTimeout();
        }
    }

    private void checkTimeout() {
        Duration d = getDuration();
        if (d.compareTo(timeoutLimit) > 0) {
            logger.error(INFO_PLACEHOLDER, infoPrefix, "Time limit has been reached");
            instances.forEach(ServiceInstance::stop);
        }
    }

    private void setBlocked() {
        status = ServiceStatus.BLOCKED;
        logger.info(INFO_PLACEHOLDER, infoPrefix, "Blocked");
    }

    private void setRunning() {
        initialInstant = Instant.now();
        status = ServiceStatus.RUNNING;
        logger.info(INFO_PLACEHOLDER, infoPrefix, "Running");
    }

    private void setFailure(String message) {
        finalInstant = Instant.now();
        status = ServiceStatus.FAILURE;
        logger.info(INFO_PLACEHOLDER, infoPrefix, message);
    }

    private void setSuccess() {
        finalInstant = Instant.now();
        status = ServiceStatus.SUCCESS;
        logger.info(INFO_PLACEHOLDER, infoPrefix, "Succeeded");
        // TODO log exit code information
    }

    public Duration getDuration() {
        if (duration == null) {
            if (status.isDisabled() || status.isBlocked()) {
                duration = Duration.ZERO;
                return duration;
            } else if (status.isSuccess() || status.isFailure()) {
                duration = Duration.between(initialInstant, finalInstant);
                return duration;
            } else if (status.isWaiting()) {
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

    public Set<ServiceDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public String getName() {
        return name;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public String getExpression() {
        return expression;
    }

    public Boolean getExpressionResult() {
        return expressionResult;
    }

    public Integer getScale() {
        return scale;
    }

    public Instant getInitialInstant() {
        return initialInstant;
    }

    public Duration getTimeoutLimit() {
        return timeoutLimit;
    }

    public Instant getFinalInstant() {
        return finalInstant;
    }

    public boolean isEnabled() {
        return status != ServiceStatus.DISABLED;
    }

    public boolean isWaitingOrRunning() {
        return status == ServiceStatus.WAITING || status == ServiceStatus.RUNNING;
    }
}
