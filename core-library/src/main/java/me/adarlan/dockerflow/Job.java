package me.adarlan.dockerflow;

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
import me.adarlan.dockerflow.bash.BashScript;

@EqualsAndHashCode(of = "name")
@ToString(of = "name")
public class Job {

    @Getter
    private final Pipeline pipeline;

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
    private Set<Rule> rules;

    // Set<Job> directDependencies;
    // Set<Job> allDependencies;
    // Integer dependencyLevel;
    // Set<Job> allDependents;

    @Getter
    private Integer scale;

    private final List<JobInstance> instances = new ArrayList<>();

    @Getter
    private Instant initialInstant = null;

    @Getter
    private Instant finalInstant = null;

    private Duration duration = null;

    @Getter
    private Duration timeout;

    final List<String> logs = new ArrayList<>();

    @Getter
    private JobStatus status;

    private Thread buildOrPullImage = null;
    private boolean imageBuiltOrPulled = false;
    private boolean startedInstances = false;
    private boolean ended = false;

    private final Logger logger = Logger.getLogger();

    Job(Pipeline pipeline, String name) {
        this.pipeline = pipeline;
        this.dockerCompose = pipeline.getDockerCompose();
        this.name = name;
    }

    void initializeTimeout(Long amount, ChronoUnit unit) {
        this.timeout = Duration.of(amount, unit);
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
            JobInstance instance = new JobInstance(this, instanceNumber);
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

    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public List<JobInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public Set<Rule> getRules() {
        return Collections.unmodifiableSet(rules);
    }

    public boolean hasEnded() {
        return ended;
    }

    void refresh() {
        synchronized (this) {
            if (!ended) {
                if (status == JobStatus.WAITING) {
                    checkRulesAndSetRunningOrBlocked();
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
                            logger.log(this, "Job interrupted when building/pulling image");
                            setStatus(JobStatus.FAILURE);
                        }
                    }
                }
            }
        }
    }

    private void checkRulesAndSetRunningOrBlocked() {
        boolean passed = true;
        boolean blocked = false;
        for (final Rule rule : rules) {
            if (rule.updateStatus()) {
                logger.info(rule);
            }
            if (!rule.getStatus().equals(RuleStatus.PASSED))
                passed = false;
            if (rule.getStatus().equals(RuleStatus.BLOCKED))
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
                if (!dockerCompose.buildImage(Job.this)) {
                    setStatus(JobStatus.FAILURE);
                }
            } else {
                if (!dockerCompose.pullImage(Job.this)) {
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
        instances.forEach(JobInstance::start);
        startedInstances = true;
    }

    private void checkInstancesAndSetSuccessOrFailure() {
        boolean success = true;
        boolean failure = false;
        for (JobInstance instance : instances) {
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
        if (d.compareTo(timeout) > 0) {
            logger.log(this, "Time limit has been reached");
            instances.forEach(JobInstance::stop);
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
        logger.info(this);
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