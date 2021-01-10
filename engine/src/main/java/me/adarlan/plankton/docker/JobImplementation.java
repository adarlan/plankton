package me.adarlan.plankton.docker;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.adarlan.plankton.core.Job;
import me.adarlan.plankton.core.JobDependency;
import me.adarlan.plankton.core.JobDependencyStatus;
import me.adarlan.plankton.core.JobInstance;
import me.adarlan.plankton.core.JobStatus;
import me.adarlan.plankton.core.Logger;

@EqualsAndHashCode(of = "name")
@ToString(of = "name")
public class JobImplementation implements Job {

    @Getter
    private final PipelineImplementation pipeline;

    private final DockerCompose dockerCompose;

    @Getter
    private final String name;

    @Setter(AccessLevel.PACKAGE)
    private boolean needToBuild;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String expression;

    @Getter
    private Boolean expressionResult;

    @Setter(AccessLevel.PACKAGE)
    private Set<JobDependency> dependencies;

    @Getter
    private Integer scale;

    private final List<JobInstanceImplementation> instances = new ArrayList<>();

    @Getter
    private Instant initialInstant = null;

    @Getter
    private Instant finalInstant = null;

    private Duration duration = null;

    @Getter
    private Duration timeoutLimit;

    private final List<String> logs = new ArrayList<>();

    @Getter
    private JobStatus status;

    private Thread buildOrPullImage = null;
    private boolean imageBuiltOrPulled = false;
    private boolean startedInstances = false;
    private boolean ended = false;

    private final Logger logger = Logger.getLogger();

    JobImplementation(PipelineImplementation pipeline, String name) {
        this.pipeline = pipeline;
        this.dockerCompose = pipeline.dockerCompose;
        this.name = name;
    }

    void initializeTimeout(Long amount, ChronoUnit unit) {
        this.timeoutLimit = Duration.of(amount, unit);
    }

    void initializeStatus() {
        boolean enable = true;
        if (expression != null) {
            evaluateExpression();
            enable = expressionResult;
        }
        if (enable) {
            setStatus(JobStatus.WAITING);
        } else {
            setStatus(JobStatus.DISABLED);
        }
    }

    void setScale(int scale) {
        this.scale = scale;
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            JobInstanceImplementation instance = new JobInstanceImplementation(this, instanceNumber);
            instances.add(instance);
        }
    }

    private void evaluateExpression() {
        final String scriptName = "evaluateExpression_" + name;
        BashScript script = new BashScript(scriptName);
        script.command(expression);
        script.run();
        if (script.getExitCode() == 0) {
            expressionResult = true;
        } else {
            expressionResult = false;
        }
    }

    void log(String message) {
        synchronized (logs) {
            logs.add(message);
        }
        logger.follow(this, () -> message);
    }

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public List<JobInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    @Override
    public Set<JobDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    @Override
    public Boolean hasEnded() {
        return ended;
    }

    void refresh() {
        synchronized (this) {
            if (!ended) {
                if (status == JobStatus.WAITING) {
                    checkDependenciesAndSetRunningOrBlocked();
                }
                if (status == JobStatus.RUNNING) {
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
                            log("Job interrupted when building/pulling image");
                            setStatus(JobStatus.FAILURE);
                        }
                    }
                }
            }
        }
    }

    private void checkDependenciesAndSetRunningOrBlocked() {
        boolean passed = true;
        boolean blocked = false;
        for (final JobDependency dependency : dependencies) {
            if (dependency.updateStatus()) {
                logger.jobDependencyInfo(dependency);
            }
            if (!dependency.getStatus().equals(JobDependencyStatus.PASSED))
                passed = false;
            if (dependency.getStatus().equals(JobDependencyStatus.BLOCKED))
                blocked = true;
        }
        if (passed) {
            setStatus(JobStatus.RUNNING);
        } else if (blocked) {
            setStatus(JobStatus.BLOCKED);
        }
    }

    private class BuildOrPullImage extends Thread {

        private BuildOrPullImage() {
            super();
        }

        @Override
        public void run() {
            if (needToBuild) {
                if (!dockerCompose.buildImage(JobImplementation.this)) {
                    setStatus(JobStatus.FAILURE);
                }
            } else {
                if (!dockerCompose.pullImage(JobImplementation.this)) {
                    setStatus(JobStatus.FAILURE);
                }
            }
            imageBuiltOrPulled = true;
        }
    }

    private void createContainers() {
        if (!dockerCompose.createContainers(this)) {
            setStatus(JobStatus.FAILURE);
        }
    }

    private void startInstances() {
        instances.forEach(JobInstanceImplementation::start);
        startedInstances = true;
    }

    private void checkInstancesAndSetSuccessOrFailure() {
        boolean success = true;
        boolean failure = false;
        for (JobInstanceImplementation instance : instances) {
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
            setStatus(JobStatus.SUCCESS);
        } else if (failure) {
            setStatus(JobStatus.FAILURE);
        } else {
            checkTimeout();
        }
    }

    private void checkTimeout() {
        Duration d = getDuration();
        if (d.compareTo(timeoutLimit) > 0) {
            log("Time limit has been reached");
            instances.forEach(JobInstanceImplementation::stop);
        }
    }

    private void setStatus(JobStatus status) {
        switch (status) {
            case DISABLED:
                ended = true;
                break;
            case WAITING:
                break;
            case BLOCKED:
                ended = true;
                break;
            case RUNNING:
                initialInstant = Instant.now();
                break;
            case FAILURE:
            case SUCCESS:
                ended = true;
                finalInstant = Instant.now();
                duration = Duration.between(initialInstant, finalInstant);
                break;
        }
        this.status = status;
        logger.jobInfo(this);
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
}