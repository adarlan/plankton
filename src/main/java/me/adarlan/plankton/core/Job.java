package me.adarlan.plankton.core;

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

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.compose.DependsOnCondition;
import me.adarlan.plankton.util.Colors;
import me.adarlan.plankton.util.LogUtils;

@EqualsAndHashCode(of = { "pipeline", "name" })
public class Job {

    // TODO for 'service_started' and 'service_healthy' dependency conditions
    // -> stop the required service when dependencies exit

    final Pipeline pipeline;
    final String name;

    final ComposeDocument compose;
    final ContainerRuntimeAdapter adapter;
    final ComposeService service;

    private Thread threadForCreateContainers;

    JobStatus status = JobStatus.WAITING;
    // TODO replace by boolean flags
    // the status should be present only on DTOs

    final Map<Job, DependsOnCondition> dependencyMap = new HashMap<>();
    Integer dependencyLevel;
    final Set<Job> directDependents = new HashSet<>();

    Integer scale;
    final List<JobInstance> instances = new ArrayList<>();

    private boolean allInstancesExitedZero = false;
    private boolean anyInstanceExitedZero = false;

    private boolean allInstancesExitedNonZero = false;
    private boolean anyInstanceExitedNonZero = false;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    private Thread timeoutCountdown = null;

    private boolean dependenciesSatisfied = false;
    private boolean dependenciesBlocked = false;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    final String colorizedName;
    final String logPrefix;

    Job(Pipeline pipeline, ComposeService service) {
        this.pipeline = pipeline;
        this.compose = pipeline.compose;
        this.adapter = pipeline.adapter;
        this.service = service;
        this.name = service.name();
        this.colorizedName = Colors.colorized(name);
        logPrefix = LogUtils.prefixOf(name);
    }

    @Override
    public String toString() {
        return colorizedName;
    }

    void start() {
        Thread thread = new Thread(() -> {
            if (!dependencyMap.isEmpty()) {
                logger.info("{}Waiting for dependencies: {}", logPrefix, dependencyMap);
                waitForDependencies();
                if (dependenciesSatisfied) {
                    logger.info("{}All dependencies satisfied", logPrefix);
                } else {
                    logger.info("{}Blocked by dependencies", logPrefix);
                    status = JobStatus.BLOCKED;
                    pipeline.refresh();
                    return;
                }
            }
            logger.debug("{}Setting status to RUNNING", logPrefix);
            status = JobStatus.RUNNING;
            startTimer();
            waitCreateContainers();
            startInstances();
        });
        thread.setUncaughtExceptionHandler((t, e) -> {
            throw new PipelineException(this, "Unable to start", e);
        });
        thread.start();
    }

    private void waitForDependencies() {
        while (!(dependenciesSatisfied || dependenciesBlocked)) {
            checkDependencies();
            if (dependenciesSatisfied || dependenciesBlocked) {
                return;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PipelineException(this, "Interrupted while waiting for dependencies", e);
                }
            }
        }
    }

    private void checkDependencies() {
        dependenciesSatisfied = true;
        dependenciesBlocked = false;
        dependencyMap.forEach((dependsOnJob, condition) -> {
            if (!dependsOnJob.satisfiesCondition(condition)) {
                dependenciesSatisfied = false;
            }
            if (dependsOnJob.blockedByCondition(condition)) {
                dependenciesBlocked = true;
            }
        });
    }

    private synchronized boolean satisfiesCondition(DependsOnCondition condition) {
        switch (condition) {
            case EXIT_ZERO:
                return allInstancesExitedZero;
            case EXIT_NON_ZERO:
                return anyInstanceExitedNonZero;
            default:
                return false;
        }
    }

    private synchronized boolean blockedByCondition(DependsOnCondition condition) {
        switch (condition) {
            case EXIT_ZERO:
                return anyInstanceExitedNonZero;
            case EXIT_NON_ZERO:
                return allInstancesExitedZero;
            default:
                return false;
        }
    }

    private void startTimer() {
        initialInstant = Instant.now();
        logger.debug("{}Initial instant: {}", logPrefix, initialInstant);
        logger.debug("{}Starting timeout countdown", logPrefix);
        timeoutCountdown = new Thread(() -> {
            try {
                Thread.sleep(pipeline.timeoutLimitForJobs.toMillis());
                logger.error("{}Timeout limit has been reached", logPrefix);
                stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("{}Timeout countdown stopped", logPrefix);
            }
        });
        timeoutCountdown.start();
        logger.debug("{}Timeout countdown started", logPrefix);
    }

    private void stopTimer() {
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

    private boolean startedToCreateContainers = false;
    private boolean finishedToCreateContainers = false;

    private void createContainers() {
        startedToCreateContainers = true;
        logger.debug("{}Started to create containers", logPrefix);
        adapter.createContainers(service);
        finishedToCreateContainers = true;
        logger.debug("{}Finished to create containers", logPrefix);
    }

    private synchronized void startThreadForCreateContainers() {
        if (threadForCreateContainers != null) {
            return;
        }
        threadForCreateContainers = new Thread(this::createContainers);
        threadForCreateContainers.setUncaughtExceptionHandler((t, e) -> {
            throw new PipelineException(this, "Uncaught exception while creating containers", e);
        });
        logger.debug("{}Starting thread for create containers", logPrefix);
        threadForCreateContainers.start();
    }

    void waitCreateContainers() {
        if (threadForCreateContainers == null) {
            startThreadForCreateContainers();
        }
        try {
            threadForCreateContainers.join();
            if (!finishedToCreateContainers) {
                throw new PipelineException(this, "Containers not created");
            }
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            throw new PipelineException(this, "Interrupted while creating containers", e1);
        }
    }

    private void startInstances() {
        instances.forEach(JobInstance::start);
    }

    void refresh() {
        checkInstancesAndSetSucceededOrFailed();
    }

    private synchronized void checkInstancesAndSetSucceededOrFailed() {
        allInstancesExitedZero = true;
        allInstancesExitedNonZero = true;
        Set<Integer> exitCodes = new HashSet<>();
        for (JobInstance instance : instances) {
            if (instance.exited()) {
                int exitCode = instance.exitCode();
                exitCodes.add(exitCode);
                if (exitCode == 0) {
                    anyInstanceExitedZero = true;
                    allInstancesExitedNonZero = false;
                } else {
                    anyInstanceExitedNonZero = true;
                    allInstancesExitedZero = false;
                }
            } else {
                allInstancesExitedZero = false;
                allInstancesExitedNonZero = false;
            }
        }
        if (allInstancesExitedZero || allInstancesExitedNonZero)
            exited(exitCodes);
    }

    private void exited(Set<Integer> exitCodes) {
        stopTimer();
        String logPlaceholder = "{}Exited: {}; Duration: {}min {}sec";
        if (allInstancesExitedZero) {
            status = JobStatus.SUCCEEDED;
            logger.info(logPlaceholder, logPrefix, "0", duration.toMinutesPart(), duration.toSecondsPart());
        } else if (allInstancesExitedNonZero) {
            status = JobStatus.FAILED;
            String x = exitCodes.stream().map(Object::toString).collect(Collectors.joining(", "));
            logger.info(logPlaceholder, logPrefix, x, duration.toMinutesPart(), duration.toSecondsPart());
        }
        pipeline.refresh();
    }

    void stop() {
        logger.info("{}Stopping", logPrefix);
        instances.forEach(JobInstance::stop);
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

    public Integer scale() {
        return scale;
    }

    public Instant initialInstant() {
        return initialInstant;
    }

    public Instant finalInstant() {
        return finalInstant;
    }

    public boolean isEnabled() {
        return !status.isDisabled();
    }

    public boolean isWaitingOrRunning() {
        return status.isWaiting() || status.isRunning();
    }

    public int dependencyLevel() {
        return dependencyLevel;
    }
}