package plankton.pipeline;

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
import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;
import plankton.util.Colors;

@EqualsAndHashCode(of = { "pipeline", "name" })
public class Job {

    Pipeline pipeline;
    String name;
    JobStatus status = JobStatus.CREATED;
    boolean elected = false;
    ComposeService composeService;

    final Map<Job, DependsOnCondition> dependencies = new HashMap<>();
    final Map<Job, DependsOnCondition> dependents = new HashMap<>();

    Integer dependencyLevel;

    final List<JobInstance> instances = new ArrayList<>();

    boolean autoStopWhenDirectDependentsHaveFinalStatus = false;

    private String colorizedName;
    private String logPrefix;
    String blueLabel;
    String greenLabel;
    String redLabel;
    String separator;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    Job() {
        super();
    }

    void initializeColorizedNameAndLogPlaceholders() {
        colorizedName = LogUtils.colorized(name);
        logPrefix = LogUtils.prefixOf(name);
        blueLabel = logPrefix + Colors.BLUE;
        greenLabel = logPrefix + Colors.GREEN;
        redLabel = logPrefix + Colors.RED;
        separator = " " + Colors.ANSI_RESET;
    }

    private Thread thread = null;

    void start() {
        logger.debug("Starting {}", this);
        thread = new Thread(() -> {
            if (!dependencies.isEmpty()) {
                setStatusWaiting();
                waitForDependencies();
                if (!blockedByDependencies.isEmpty()) {
                    setFinalStatusBlocked();
                    return;
                }
            }
            if (composeService.build().isPresent()) {
                setStatusBuilding();
                pipeline.containerRuntimeAdapter
                        .buildImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}BUILDING_IMAGE{}{}", blueLabel, separator, msg))
                                .forEachError(msg -> logger.error("{}BUILDING_IMAGE{}{}", blueLabel, separator, msg))
                                .build());
            } else {
                setStatusPulling();
                pipeline.containerRuntimeAdapter
                        .pullImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}PULLING_IMAGE{}{}", blueLabel, separator, msg))
                                .forEachError(msg -> logger.error("{}PULLING_IMAGE{}{}", blueLabel, separator, msg))
                                .build());
            }
            if (composeService.build().isPresent() && composeService.image().isPresent()
                    && composeService.entrypointIsReseted() && composeService.command().isEmpty()) {
                setFinalStatusBuilt();
            } else {
                setStatusRunning();
                pipeline.containerRuntimeAdapter.createContainers(ContainerConfiguration.builder()
                        .service(composeService)
                        .forEachOutput(msg -> logger.info("{}STARTING_CONTAINER{}{}", blueLabel, separator, msg))
                        .forEachError(msg -> logger.error("{}STARTING_CONTAINER{}{}", blueLabel, separator, msg))
                        .build());
                instances.forEach(JobInstance::start);
            }
        });
        thread.setUncaughtExceptionHandler(
                (t, e) -> {
                    e.printStackTrace();
                    setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage());
                });
        thread.start();
    }

    private final Set<JobInstance> exitedInstances = new HashSet<>();
    private final Set<Integer> exitCodes = new HashSet<>();
    private boolean anyInstanceExitedNonZero = false;

    void exited(JobInstance instance) {
        logger.debug("Job {} is being notified because instance {} exited", this, instance.index);
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
        logger.debug("Stopping job {}", this);
        if (thread != null) {
            instances.forEach(JobInstance::stop);
            thread.interrupt();
        }
    }

    private final Map<Job, DependsOnCondition> satisfiedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> blockedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> waitingForDependencies = new HashMap<>();

    private void waitForDependencies() {
        logger.debug("{} waiting for dependencies: {}", this, dependencies);
        dependencies.forEach(waitingForDependencies::put);
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
            logger.debug("{} dependencies satisfied: {}", this, dependencies);
    }

    private void checkDependencies() {
        satisfiedByDependencies.clear();
        blockedByDependencies.clear();
        waitingForDependencies.clear();
        dependencies.forEach((dependsOnJob, condition) -> {
            if (dependsOnJob.satisfiesCondition(condition))
                satisfiedByDependencies.put(dependsOnJob, condition);
            else if (dependsOnJob.blockedByCondition(condition))
                blockedByDependencies.put(dependsOnJob, condition);
            else
                waitingForDependencies.put(dependsOnJob, condition);
        });
        if (!satisfiedByDependencies.isEmpty())
            logger.debug("{} satisfied by dependencies: {}", this, satisfiedByDependencies);
        if (!blockedByDependencies.isEmpty())
            logger.debug("{} blocked by dependencies: {}", this, blockedByDependencies);
        if (!waitingForDependencies.isEmpty())
            logger.debug("{} waiting for dependencies: {}", this, waitingForDependencies);
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
                throw new UnsupportedOperationException("Unsupported dependency condition: " + condition);
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
                throw new UnsupportedOperationException("Unsupported dependency condition: " + condition);
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
        String blockedBy = blockedByDependencies
                .keySet()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        logger.error("{}FAILED{}Blocked by dependencies: {}", redLabel, separator, blockedBy);
        pipeline.refresh();
    }

    private void setFinalStatusBuilt() {
        status = JobStatus.BUILT;
        stopTimer();
        String image = composeService.image().orElseThrow();
        String time = durationAsString();
        logger.info("{}COMPLETED_SUCCESSFULLY{}Image built: {}; Time: {}", greenLabel, separator, image, time);
        pipeline.refresh();
    }

    private void setFinalStatusExitedZero() {
        status = JobStatus.EXITED_ZERO;
        stopTimer();
        String time = durationAsString();
        logger.info("{}COMPLETED_SUCCESSFULLY{}Exit code: 0; Time: {}", greenLabel, separator, time);
        pipeline.refresh();
    }

    private void setFinalStatusExitedNonZero() {
        status = JobStatus.EXITED_NON_ZERO;
        stopTimer();
        String codes = exitCodes.stream()
                .filter(n -> n != 0)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        String time = durationAsString();
        logger.error("{}FAILED{}Exited non-zero code: {}; Time: {}", redLabel, separator, codes, time);
        pipeline.refresh();
    }

    private String errorMessage;

    void setFinalStatusError(String message) {
        status = JobStatus.ERROR;
        if (timerStarted)
            stopTimer();
        this.errorMessage = message;
        logger.error("{}FAILED{}{}", redLabel, separator, errorMessage);
        instances.forEach(JobInstance::stop);
        pipeline.refresh();
    }

    private String durationAsString() {
        return duration.getSeconds() + "sec";
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
        logger.debug("{} initial instant: {}", this, initialInstant);
        logger.debug("{} starting timeout countdown", this);
        timeoutCountdown = new Thread(() -> {
            try {
                Thread.sleep(pipeline.timeoutLimitForJobs.toMillis());
                logger.error("{}TIMEOUT{}Time limit has been reached", redLabel, separator);
                stop();
                finalInstant = Instant.now();
                logger.debug("{} final instant: {}", this, finalInstant);
                duration = Duration.between(initialInstant, finalInstant);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (timerStopped)
                    logger.debug("{} timeout countdown stopped", this);
                else
                    setFinalStatusError("Timeout countdown interrupted");
            }
        });
        timeoutCountdown.setUncaughtExceptionHandler(
                (t, e) -> setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage()));
        timeoutCountdown.start();
        logger.debug("{} timeout countdown started", this);
    }

    private void stopTimer() {
        timerStopped = true;
        if (initialInstant != null) {
            finalInstant = Instant.now();
            logger.debug("{} final instant: {}", this, finalInstant);
            duration = Duration.between(initialInstant, finalInstant);
        }
        if (timeoutCountdown != null) {
            logger.debug("{} stopping timeout countdown", this);
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

    public Map<Job, DependsOnCondition> dependencies() {
        return Collections.unmodifiableMap(dependencies);
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
        return colorizedName == null
                ? name
                : colorizedName;
    }
}
