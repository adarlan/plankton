package plankton.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import plankton.adapter.ContainerRuntimeAdapter;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;
import plankton.util.Colors;
import plankton.util.LogUtils;

@EqualsAndHashCode(of = { "pipeline", "name" })
public class Job {

    final Pipeline pipeline;
    final String name;
    JobStatus status;

    final ComposeDocument composeDocument;
    final ComposeService composeService;
    final ContainerRuntimeAdapter containerRuntimeAdapter;

    final Map<Job, DependsOnCondition> dependencyMap = new HashMap<>();
    Integer dependencyLevel;
    final Set<Job> directDependents = new HashSet<>();
    final List<JobInstance> instances = new ArrayList<>();

    final Set<DependsOnCondition> requiredConditions = new HashSet<>();
    boolean autoStopWhenDirectDependentsHaveFinalStatus = false;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    final String colorizedName;
    final String logPrefix;

    Job(Pipeline pipeline1, ComposeService composeService1) {
        this.pipeline = pipeline1;
        this.composeService = composeService1;
        this.composeDocument = pipeline.composeDocument;
        this.containerRuntimeAdapter = pipeline.containerRuntimeAdapter;
        this.name = composeService.name();
        this.colorizedName = Colors.colorized(name);
        this.logPrefix = LogUtils.prefixOf(name);
    }

    private Thread thread = null;

    void start() {
        logger.debug("{}Starting", logPrefix);
        thread = new Thread(() -> {
            if (!dependencyMap.isEmpty()) {
                setStatusWaiting();
                waitForDependencies();
                if (!blockedByDependencies.isEmpty()) {
                    setFinalStatusBlocked();
                    return;
                }
            }
            if (composeService.build().isPresent()) {
                setStatusBuilding();
                containerRuntimeAdapter.buildImage(composeService);
            } else {
                setStatusPulling();
                containerRuntimeAdapter.pullImage(composeService);
            }
            if (composeService.build().isPresent() && composeService.image().isPresent()
                    && composeService.entrypointIsReseted() && composeService.command().isEmpty()) {
                setFinalStatusBuilt();
            } else {
                setStatusRunning();
                containerRuntimeAdapter.createContainers(composeService);
                instances.forEach(JobInstance::start);
            }
        });
        thread.setUncaughtExceptionHandler(
                (t, e) -> setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage()));
        thread.start();
    }

    private final Set<JobInstance> exitedInstances = new HashSet<>();
    private final Set<Integer> exitCodes = new HashSet<>();
    private boolean anyInstanceExitedNonZero = false;

    void exited(JobInstance instance) {
        logger.debug("{}Being notified because instance {} exited", logPrefix, instance);
        exitedInstances.add(instance);
        Integer exitCode = instance.exitCode();
        exitCodes.add(exitCode);
        if (exitCode != 0)
            anyInstanceExitedNonZero = true;
        if (exitedInstances.size() == instances.size()) {
            if (!anyInstanceExitedNonZero)
                setFinalStatusExitedZero();
            else
                setFinalStatusExitedNonZero();
        }
    }

    void stop() {
        logger.debug("{}Stopping", logPrefix);
        if (thread != null) {
            instances.forEach(JobInstance::stop);
            thread.interrupt();
        }
    }

    // private boolean dependenciesSatisfied = false;
    // private boolean dependenciesBlocked = false;
    private final Map<Job, DependsOnCondition> satisfiedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> blockedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> waitingForDependencies = new HashMap<>();

    private void waitForDependencies() {
        logger.info("{}{}Waiting for dependencies{}: {}", logPrefix, Colors.BRIGHT_WHITE, Colors.ANSI_RESET,
                dependencyMap);
        dependencyMap.forEach(waitingForDependencies::put);
        while (!waitingForDependencies.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                setFinalStatusError("Interrupted while waiting for dependencies");
                break;
            }
            checkDependencies();
            if (!waitingForDependencies.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (blockedByDependencies.isEmpty())
            logger.info("{}{}Dependencies satisfied{}: {}", logPrefix, Colors.BRIGHT_WHITE, Colors.ANSI_RESET,
                    dependencyMap);
    }

    private void checkDependencies() {
        satisfiedByDependencies.clear();
        blockedByDependencies.clear();
        waitingForDependencies.clear();
        dependencyMap.forEach((dependsOnJob, condition) -> {
            if (dependsOnJob.satisfiesCondition(condition))
                satisfiedByDependencies.put(dependsOnJob, condition);
            else if (dependsOnJob.blockedByCondition(condition))
                blockedByDependencies.put(dependsOnJob, condition);
            else
                waitingForDependencies.put(dependsOnJob, condition);
        });
        if (!satisfiedByDependencies.isEmpty())
            logger.debug("{}Satisfied by dependencies: {}", logPrefix, satisfiedByDependencies);
        if (!blockedByDependencies.isEmpty())
            logger.debug("{}Blocked by dependencies: {}", logPrefix, blockedByDependencies);
        if (!waitingForDependencies.isEmpty())
            logger.debug("{}Waiting for dependencies: {}", logPrefix, waitingForDependencies);
    }

    private synchronized boolean satisfiesCondition(DependsOnCondition condition) {
        switch (condition) {
            case SERVICE_STARTED:
                return started;
            case SERVICE_COMPLETED_SUCCESSFULLY:
                return status.isSucceeded();
            case SERVICE_FAILED:
                return status.isFailed();
            default:
                throw new PipelineException(this, "Unsupported dependency condition: " + condition);
        }
    }

    private synchronized boolean blockedByCondition(DependsOnCondition condition) {
        switch (condition) {
            case SERVICE_STARTED:
                return status.isFinal() && !started;
            case SERVICE_COMPLETED_SUCCESSFULLY:
                return status.isFailed();
            case SERVICE_FAILED:
                return status.isSucceeded();
            default:
                throw new PipelineException(this, "Unsupported dependency condition: " + condition);
        }
    }

    private boolean started = false;

    private void setStatusWaiting() {
        status = JobStatus.WAITING;
    }

    private void setStatusBuilding() {
        status = JobStatus.BUILDING;
        startTimer();
    }

    private void setStatusPulling() {
        status = JobStatus.PULLING;
        startTimer();
    }

    private void setStatusRunning() {
        status = JobStatus.RUNNING;
        started = true;
        if (!timerStarted)
            startTimer();
    }

    private void setFinalStatusBlocked() {
        status = JobStatus.BLOCKED;
        logFinalStatus();
        pipeline.refresh();
    }

    private void setFinalStatusBuilt() {
        status = JobStatus.BUILT;
        stopTimer();
        logFinalStatus();
        pipeline.refresh();
    }

    private void setFinalStatusExitedZero() {
        status = JobStatus.EXITED_ZERO;
        stopTimer();
        logFinalStatus();
        pipeline.refresh();
    }

    private void setFinalStatusExitedNonZero() {
        status = JobStatus.EXITED_NON_ZERO;
        stopTimer();
        logFinalStatus();
        pipeline.refresh();
    }

    private String errorMessage;

    void setFinalStatusError(String message) {
        status = JobStatus.ERROR;
        if (timerStarted)
            stopTimer();
        this.errorMessage = message;
        logFinalStatus();
        instances.forEach(JobInstance::stop);
        pipeline.refresh();
    }

    void logFinalStatus() {

        String time = "";
        if (duration != null)
            time = " (" + duration.toMinutesPart() + "min " + duration.toSecondsPart() + "sec)";
        // TODO what if more than 1 hour?

        String prefix;
        if (status.isSucceeded())
            prefix = logPrefix + Colors.GREEN + "SUCCEEDED" + Colors.ANSI_RESET + " " + Colors.BRIGHT_BLACK + "..."
                    + Colors.ANSI_RESET + " ";
        else
            prefix = logPrefix + Colors.RED + "FAILED" + Colors.ANSI_RESET + " " + Colors.BRIGHT_BLACK + "......"
                    + Colors.ANSI_RESET + " ";

        if (status.isBlocked())
            logger.error("{}Blocked by dependencies: {}", prefix, blockedByDependencies);

        else if (status.isBuilt()) {
            String image = composeService.image().orElse("");
            logger.info("{}Image built: {}{}", prefix, image, time);

        } else if (status.isExitedZero())
            logger.info("{}Exited: 0{}", prefix, time);

        else if (status.isExitedNonZero()) {
            String x = exitCodes.stream().map(Object::toString).collect(Collectors.joining(", "));
            logger.error("{}Exited: {}{}", prefix, x, time);

        } else if (status.isError())
            logger.error("{}{}{}", prefix, errorMessage, time);
    }

    private boolean timerStarted = false;
    private boolean timerStopped = false;
    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    private Thread timeoutCountdown = null;

    private void startTimer() {
        timerStarted = true;
        initialInstant = Instant.now();
        logger.debug("{}Initial instant: {}", logPrefix, initialInstant);
        logger.debug("{}Starting timeout countdown", logPrefix);
        timeoutCountdown = new Thread(() -> {
            try {
                Thread.sleep(pipeline.timeoutLimitForJobs.toMillis());
                logger.error("{}{}Timeout limit has been reached{}", logPrefix, Colors.RED, Colors.ANSI_RESET);
                stop();
                finalInstant = Instant.now();
                logger.debug("{}Final instant: {}", logPrefix, finalInstant);
                duration = Duration.between(initialInstant, finalInstant);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (timerStopped)
                    logger.debug("{}Timeout countdown stopped", logPrefix);
                else
                    setFinalStatusError("Timeout countdown interrupted");
            }
        });
        timeoutCountdown.setUncaughtExceptionHandler(
                (t, e) -> setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage()));
        timeoutCountdown.start();
        logger.debug("{}Timeout countdown started", logPrefix);
    }

    private void stopTimer() {
        timerStopped = true;
        if (initialInstant != null) {
            finalInstant = Instant.now();
            logger.debug("{}Final instant: {}", logPrefix, finalInstant);
            duration = Duration.between(initialInstant, finalInstant);
        }
        if (timeoutCountdown != null) {
            logger.debug("{}Stopping timeout countdown", logPrefix);
            timeoutCountdown.interrupt();
        }
    }

    public Duration duration() {
        if (duration != null) {
            return duration;
        } else if (initialInstant == null) {
            return Duration.ZERO;
        } else {
            return Duration.between(initialInstant, Instant.now());
        }
    }

    public List<JobInstance> instances() {
        return Collections.unmodifiableList(instances);
    }

    public Map<Job, DependsOnCondition> dependencyMap() {
        return Collections.unmodifiableMap(dependencyMap);
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

    public Instant initialInstant() {
        return initialInstant;
    }

    public Instant finalInstant() {
        return finalInstant;
    }

    public Integer dependencyLevel() {
        return dependencyLevel;
    }

    @Override
    public String toString() {
        return colorizedName;
    }
}
