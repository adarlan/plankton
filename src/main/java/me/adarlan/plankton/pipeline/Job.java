package me.adarlan.plankton.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.compose.DependsOnCondition;

@EqualsAndHashCode(of = { "pipeline", "name" })
@ToString(of = { "name" })
public class Job {

    final Pipeline pipeline;
    final String name;

    final ComposeDocument compose;
    final ContainerRuntimeAdapter adapter;
    final ComposeService service;

    JobStatus status;

    final Map<Job, DependsOnCondition> dependencyMap = new HashMap<>();
    Integer dependencyLevel;

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

    String color;
    String prefix;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    Job(Pipeline pipeline, ComposeService service) {
        this.pipeline = pipeline;
        this.compose = pipeline.compose;
        this.adapter = pipeline.adapter;
        this.service = service;
        this.name = service.name();
    }

    private boolean dependenciesSatisfied = false;
    private boolean dependenciesBlocked = false;

    void start() {
        Thread thread = new Thread(() -> {
            if (!dependencyMap.isEmpty()) {
                logger.info("{} -> Waiting for dependencies", this);
                waitForDependencies();
                if (dependenciesSatisfied) {
                    logger.info("{} -> All dependencies satisfied", this);
                } else {
                    logger.info("{} -> Blocked by dependencies", this);
                    status = JobStatus.BLOCKED;
                    pipeline.refresh();
                    return;
                }
            }
            logger.info("{} -> Running", this);
            status = JobStatus.RUNNING;
            startTimer();
            createContainers();
            startInstances();
        });
        thread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("{} -> Unable to start", this, e);
            throw new PipelineException("Unable to start: " + this, e);
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
                    logger.error("{} -> Interrupted when waiting for dependencies", this, e);
                    return;
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

    private boolean satisfiesCondition(DependsOnCondition condition) {
        switch (condition) {
            case SERVICE_EXITED_ZERO:
                return allInstancesExitedZero();
            case SERVICE_EXITED_NON_ZERO:
                return anyInstanceExitedNonZero();
            default:
                return false;
        }
    }

    private boolean blockedByCondition(DependsOnCondition condition) {
        switch (condition) {
            case SERVICE_EXITED_ZERO:
                return anyInstanceExitedNonZero();
            case SERVICE_EXITED_NON_ZERO:
                return allInstancesExitedZero();
            default:
                return false;
        }
    }

    private void startTimer() {
        initialInstant = Instant.now();
        logger.info("{} -> initialInstant={}", this, initialInstant);
        logger.info("{} -> Starting timeout countdown", Job.this);
        timeoutCountdown = new Thread(() -> {
            try {
                Thread.sleep(pipeline.timeoutLimitForJobs.toMillis());
                logger.error("{} -> Timeout limit has been reached", Job.this);
                Job.this.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("{} -> Timeout countdown has been stopped", Job.this);
            }
        });
        timeoutCountdown.start();
    }

    private void stopTimer() {
        if (initialInstant != null) {
            finalInstant = Instant.now();
            duration = Duration.between(initialInstant, finalInstant);
        }
        if (timeoutCountdown != null) {
            logger.info("{} -> Stopping timeout countdown", Job.this);
            timeoutCountdown.interrupt();
        }
    }

    private void createContainers() {
        adapter.createContainers(service);
    }

    private void startInstances() {
        instances.forEach(JobInstance::start);
    }

    void refresh() {
        synchronized (this) {
            checkInstancesAndSetSucceededOrFailed();
        }
    }

    private void checkInstancesAndSetSucceededOrFailed() {
        boolean succeeded = true;
        boolean failed = false;
        int failedNumber = 0;
        for (JobInstance instance : instances) {
            if (instance.exited()) {
                if (instance.exitCode() == 0) {
                    anyInstanceExitedZero = true;
                } else {
                    anyInstanceExitedNonZero = true;
                    failed = true;
                    failedNumber++;
                    succeeded = false;
                }
            } else {
                succeeded = false;
            }
        }
        if (succeeded) {
            logger.info("{} -> Succeeded", this);
            status = JobStatus.SUCCEEDED;
            allInstancesExitedZero = true;
            stopTimer();
            pipeline.refresh();
        } else if (failed) {
            if (scale == 1) {
                logger.info("{} -> Failed: the container returned a non-zero code", this);
            } else if (failedNumber > 1) {
                logger.info("{} -> Failed: {} containers returned a non-zero code", this, failedNumber);
            } else {
                logger.info("{} -> Failed: a container returned a non-zero code", this);
            }
            status = JobStatus.FAILED;
            allInstancesExitedNonZero = true;
            stopTimer();
            pipeline.refresh();
        }
    }

    void stop() {
        logger.info("{} -> Stopping", this);
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
        return status != JobStatus.DISABLED;
    }

    public boolean isWaitingOrRunning() {
        return status == JobStatus.WAITING || status == JobStatus.RUNNING;
    }

    public boolean allInstancesExitedZero() {
        return allInstancesExitedZero;
    }

    public boolean anyInstanceExitedZero() {
        return anyInstanceExitedZero;
    }

    public boolean allInstancesExitedNonZero() {
        return allInstancesExitedNonZero;
    }

    public boolean anyInstanceExitedNonZero() {
        return anyInstanceExitedNonZero;
    }

    public int dependencyLevel() {
        return dependencyLevel;
    }
}
