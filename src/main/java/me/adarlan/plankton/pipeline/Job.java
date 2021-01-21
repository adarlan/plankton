package me.adarlan.plankton.pipeline;

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
import me.adarlan.plankton.compose.ComposeAdapter;

@EqualsAndHashCode(of = { "pipeline", "name" })
@ToString(of = { "name" })
public class Job {

    final Pipeline pipeline;
    final String name;
    JobStatus status;

    String expression;
    Boolean expressionResult;

    final Set<JobDependency> dependencies = new HashSet<>();
    Integer dependencyLevel;

    Integer scale;
    final List<JobInstance> instances = new ArrayList<>();

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    Duration timeoutLimit;

    private Thread createContainers = null;
    private boolean hasCreatedContainers = false;
    private boolean hasStartedContainersCreation = false;

    private boolean startedInstances = false;

    String color;
    String prefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String INFO_PLACEHOLDER = "{}" + Colors.BRIGHT_WHITE + "{}" + Colors.ANSI_RESET;
    private static final String INFO_PLACEHOLDER_2 = "{}{}";

    private static final Marker LOG_MARKER = MarkerFactory.getMarker("LOG");
    private static final String LOG_PLACEHOLDER = "{}{}";

    final ComposeAdapter composeAdapter;

    private final List<String> logs = new ArrayList<>();

    Job(Pipeline pipeline, String name) {
        this.pipeline = pipeline;
        this.composeAdapter = pipeline.composeAdapter;
        this.name = name;
    }

    private void logOutput(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.info(LOG_MARKER, LOG_PLACEHOLDER, prefix, message);
    }

    private void logError(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.error(LOG_MARKER, LOG_PLACEHOLDER, prefix, message);
    }

    void refresh() {
        synchronized (this) {
            if (status.isWaiting()) {
                checkDependenciesAndSetRunningOrBlocked();
            }
            if (!status.isBlocked() && !hasStartedContainersCreation) {
                startContainersCreation();
            }
            if (hasStartedContainersCreation && !hasCreatedContainers && createContainers.isInterrupted()) {
                setFailed("Interrupted when creating container" + (scale > 1 ? "s" : ""));
            }
            if (status.isRunning() && hasCreatedContainers && !startedInstances) {
                startInstances();
            }
            if (status.isRunning() && startedInstances) {
                checkInstancesAndSetSucceededOrFailed();
            }
        }
    }

    private void checkDependenciesAndSetRunningOrBlocked() {
        boolean passed = true;
        boolean blocked = false;
        for (final JobDependency dependency : dependencies) {
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

    private void startContainersCreation() {
        createContainers = new Thread(this::createContainers);
        createContainers.start();
        hasStartedContainersCreation = true;
    }

    private void createContainers() {
        String info = "Creating container" + (scale > 1 ? "s" : "");
        logger.info(INFO_PLACEHOLDER, prefix, info);
        if (composeAdapter.createContainers(name, scale, this::logOutput, this::logError)) {
            hasCreatedContainers = true;
        } else {
            String error = "Failed when creating container" + (scale > 1 ? "s" : "");
            setFailed(error);
        }
    }

    private void startInstances() {
        instances.forEach(JobInstance::start);
        startedInstances = true;
    }

    private void checkInstancesAndSetSucceededOrFailed() {
        boolean succeeded = true;
        boolean failed = false;
        int failedNumber = 0;
        for (JobInstance instance : instances) {
            instance.refresh();
            if (instance.hasExited()) {
                Integer exitCode = instance.exitCode();
                if (exitCode == null || !exitCode.equals(0)) {
                    failed = true;
                    failedNumber++;
                    succeeded = false;
                }
            } else {
                succeeded = false;
            }
        }
        if (succeeded) {
            setSucceeded();
        } else if (failed) {
            if (scale == 1) {
                setFailed("Container returned a non-zero code");
            } else if (failedNumber > 1) {
                setFailed(failedNumber + " containers returned a non-zero code");
            } else {
                setFailed("A container returned a non-zero code");
            }
        } else {
            checkTimeout();
        }
    }

    private void checkTimeout() {
        Duration d = duration();
        if (d.compareTo(timeoutLimit) > 0) {
            logger.error(INFO_PLACEHOLDER, prefix, "Time limit has been reached");
            instances.forEach(JobInstance::stop);
        }
    }

    private void setBlocked() {
        status = JobStatus.BLOCKED;
        String m = Colors.BRIGHT_RED + status + Colors.ANSI_RESET;
        logger.error(INFO_PLACEHOLDER_2, prefix, m);
    }

    private void setRunning() {
        initialInstant = Instant.now();
        status = JobStatus.RUNNING;
        String m = Colors.BRIGHT_GREEN + status + Colors.ANSI_RESET;
        logger.info(INFO_PLACEHOLDER_2, prefix, m);
    }

    private void setFailed(String message) {
        finalInstant = Instant.now();
        status = JobStatus.FAILED;
        String m = Colors.BRIGHT_RED + status + Colors.ANSI_RESET + Colors.BRIGHT_BLACK + " -> " + Colors.ANSI_RESET
                + Colors.RED + message + Colors.ANSI_RESET;
        logger.error(INFO_PLACEHOLDER_2, prefix, m);
    }

    private void setSucceeded() {
        finalInstant = Instant.now();
        status = JobStatus.SUCCEEDED;
        String m = Colors.BRIGHT_GREEN + status + Colors.ANSI_RESET;
        logger.info(INFO_PLACEHOLDER_2, prefix, m);
    }

    public Duration duration() {
        if (duration == null) {
            if (status.isDisabled() || status.isBlocked()) {
                duration = Duration.ZERO;
                return duration;
            } else if (status.isSucceeded() || status.isFailed()) {
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

    public List<String> logs() {
        return Collections.unmodifiableList(logs);
    }

    public List<JobInstance> instances() {
        return Collections.unmodifiableList(instances);
    }

    public Set<JobDependency> dependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public Pipeline pipeline() {
        return pipeline;
    }

    public String name() {
        return name;
    }

    public JobStatus status() {
        return status;
    }

    public String expression() {
        return expression;
    }

    public Boolean expressionResult() {
        return expressionResult;
    }

    public Integer scale() {
        return scale;
    }

    public Instant initialInstant() {
        return initialInstant;
    }

    public Duration timeoutLimit() {
        return timeoutLimit;
    }

    public Instant finalInstant() {
        return finalInstant;
    }

    public boolean isEnabled() {
        return status != JobStatus.DISABLED;
    }

    public boolean isWaitingOrRunning() {
        return status == JobStatus.WAITING || status == JobStatus.RUNNING;
    }

    public int dependencyLevel() {
        return dependencyLevel;
    }
}
