package plankton.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    boolean elected = false;
    ComposeService composeService;

    final Map<Job, DependsOnCondition> dependencies = new HashMap<>();
    final Map<Job, DependsOnCondition> dependents = new HashMap<>();
    Integer dependencyLevel;
    boolean autoStopWhenDirectDependentsHaveFinalStatus = false;

    private JobStatus status = JobStatus.CREATED;

    private boolean timerStarted = false;
    private boolean timerStopped = false;
    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    private Thread timeoutCountdown = null;

    private final Map<Job, DependsOnCondition> satisfiedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> blockedByDependencies = new HashMap<>();
    private final Map<Job, DependsOnCondition> waitingForDependencies = new HashMap<>();

    private Integer exitCode = null;

    private boolean started = false;

    private Thread thread = null;

    private String errorMessage;

    private boolean running = false;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    private String colorizedName;
    private String logPrefix;
    private String blueLabel;
    private String greenLabel;
    private String redLabel;
    private String separator;
    private String infoPlaceholder;
    private String errorPlaceholder;

    Job() {
        super();
    }

    public Integer exitCode() {
        return exitCode;
    }

    void initializeColorizedNameAndLogPlaceholders() {
        colorizedName = LogUtils.colorized(name);
        logPrefix = LogUtils.prefixOf(name);
        blueLabel = logPrefix + Colors.BLUE;
        greenLabel = logPrefix + Colors.GREEN;
        redLabel = logPrefix + Colors.RED;
        separator = " " + Colors.ANSI_RESET;
        infoPlaceholder = logPrefix
                + Colors.BLUE + "INFO  " + Colors.ANSI_RESET + "{}";
        errorPlaceholder = logPrefix
                + Colors.RED + "ERROR " + Colors.ANSI_RESET + "{}";
    }

    void start() {
        logger.debug("Starting {}", this);
        thread = new Thread(() -> {
            // if (!dependencies.isEmpty()) {
            // status = JobStatus.WAITING;
            // waitForDependencies();
            // if (!blockedByDependencies.isEmpty()) {
            // status = JobStatus.BLOCKED;
            // String blockedBy = blockedByDependencies
            // .keySet()
            // .stream()
            // .map(Object::toString)
            // .collect(Collectors.joining(", "));
            // logger.error("{}FAILED{}Blocked by dependencies: {}", redLabel, separator,
            // blockedBy);
            // pipeline.notifyJobFailed(this);
            // return;
            // }
            // }
            if (composeService.build().isPresent()) {
                status = JobStatus.BUILDING;
                startTimer();
                pipeline.containerRuntimeAdapter
                        .buildImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}BUILDING_IMAGE{}{}", blueLabel, separator, msg))
                                .forEachError(msg -> logger.error("{}BUILDING_IMAGE{}{}", blueLabel, separator, msg))
                                .build());
            } else {
                status = JobStatus.PULLING;
                startTimer();
                pipeline.containerRuntimeAdapter
                        .pullImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}PULLING_IMAGE{}{}", blueLabel, separator, msg))
                                .forEachError(msg -> logger.error("{}PULLING_IMAGE{}{}", blueLabel, separator, msg))
                                .build());
            }
            if (composeService.build().isPresent() && composeService.image().isPresent()
                    && composeService.entrypointIsReseted() && composeService.command().isEmpty()) {
                status = JobStatus.BUILT;
                stopTimer();
                String image = composeService.image().orElseThrow();
                String time = durationAsString();
                logger.info("{}COMPLETED_SUCCESSFULLY{}Image built: {}; Time: {}", greenLabel, separator, image, time);
                pipeline.notifyJobCompletedSuccessfully(this);
            } else {
                status = JobStatus.RUNNING;
                started = true;
                if (!timerStarted)
                    startTimer();
                pipeline.containerRuntimeAdapter.createContainer(ContainerConfiguration.builder()
                        .service(composeService)
                        .forEachOutput(msg -> logger.info("{}STARTING_CONTAINER{}{}", blueLabel, separator, msg))
                        .forEachError(msg -> logger.error("{}STARTING_CONTAINER{}{}", blueLabel, separator, msg))
                        .build());
                logger.debug("Starting instance: {}", this);
                if (dependents.values().contains(DependsOnCondition.SERVICE_HEALTHY))
                    startHealthcheck();
                run();
            }
        });
        thread.setUncaughtExceptionHandler(
                (t, e) -> {
                    logger.error("Error", e);
                    setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage());
                });
        thread.start();
    }

    private void run() {
        initialInstant = Instant.now();
        running = true;
        pipeline.notifyJobStarted(this);
        exitCode = pipeline.containerRuntimeAdapter
                .startContainerAndGetExitCode(ContainerConfiguration.builder()
                        .service(composeService)
                        .forEachOutput(msg -> logger.info(infoPlaceholder, msg))
                        .forEachError(msg -> logger.error(errorPlaceholder, msg))
                        .build());
        finalInstant = Instant.now();
        duration = Duration.between(initialInstant, finalInstant);
        running = false;
        if (exitCode == 0) {
            status = JobStatus.EXITED_ZERO;
            stopTimer();
            String time = durationAsString();
            logger.info("{}COMPLETED_SUCCESSFULLY{}Exit code: 0; Time: {}", greenLabel, separator, time);
            pipeline.notifyJobCompletedSuccessfully(this);
        } else {
            status = JobStatus.EXITED_NON_ZERO;
            stopTimer();
            String time = durationAsString();
            logger.error("{}FAILED{}Exited non-zero code: {}; Time: {}", redLabel, separator, exitCode, time);
            pipeline.notifyJobFailed(this);
        }
    }

    void stop() {
        logger.debug("Stopping job {}", this);
        if (thread != null) {
            synchronized (this) {
                logger.debug("Stopping instance: {}", this);
                pipeline.containerRuntimeAdapter
                        .stopContainer(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info(
                                        "{}STOPPING_CONTAINER{}",
                                        redLabel, separator))
                                .forEachError(msg -> logger.error(
                                        "{}STOPPING_CONTAINER{}",
                                        redLabel, separator))
                                .build());
            }
            thread.interrupt();
        }
    }

    private void startHealthcheck() {
        Thread followState = new Thread(() -> {
            while (exitCode == null) {
                if (Thread.currentThread().isInterrupted()) {
                    setFinalStatusError("Interrupted when following container state");
                    break;
                }
                if (healthcheck()) {
                    pipeline.notifyJobHealthy(this);
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        followState.setUncaughtExceptionHandler((t, e) -> {
            logger.error("Healthcheck error", e);
            setFinalStatusError("");
        });
        followState.start();
    }

    private boolean healthcheck() {
        // TODO healthcheck
        return true;
    }

    void block() {
        status = JobStatus.BLOCKED;
        // String blockedBy = blockedByDependencies
        // .keySet()
        // .stream()
        // .map(Object::toString)
        // .collect(Collectors.joining(", "));
        logger.error("{}FAILED{}Blocked by dependencies", redLabel, separator);
        pipeline.notifyJobFailed(this);
    }

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
            default:
                throw new UnsupportedOperationException("Unsupported dependency condition: " + condition);
        }
    }

    void setFinalStatusError(String message) {
        status = JobStatus.ERROR;
        if (timerStarted)
            stopTimer();
        this.errorMessage = message;
        logger.error("{}FAILED{}{}", redLabel, separator, errorMessage);
        stop();
        pipeline.notifyJobFailed(this);
    }

    private String durationAsString() {
        return duration.getSeconds() + "sec";
    }

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
        synchronized (this) {
            if (duration != null)
                return duration;
            else if (initialInstant != null)
                return Duration.between(initialInstant, Instant.now());
            else
                return Duration.ZERO;
        }
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
