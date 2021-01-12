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
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceDependencyStatus;
import me.adarlan.plankton.core.ServiceInstance;
import me.adarlan.plankton.core.ServiceStatus;
import me.adarlan.plankton.core.Logger;
import me.adarlan.plankton.bash.BashScript;

@EqualsAndHashCode(of = "name")
@ToString(of = "name")
public class ServiceImplementation implements Service {

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
    private Set<ServiceDependency> dependencies;

    @Getter
    private Integer scale;

    private final List<ServiceInstanceImplementation> instances = new ArrayList<>();

    @Getter
    private Instant initialInstant = null;

    @Getter
    private Instant finalInstant = null;

    private Duration duration = null;

    @Getter
    private Duration timeoutLimit;

    private final List<String> logs = new ArrayList<>();

    @Getter
    private ServiceStatus status;

    private Thread buildOrPullImage = null;
    private boolean imageBuiltOrPulled = false;
    private boolean startedInstances = false;
    private boolean ended = false;

    private final Logger logger = Logger.getLogger();

    ServiceImplementation(PipelineImplementation pipeline, String name) {
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
            setStatus(ServiceStatus.WAITING);
        } else {
            setStatus(ServiceStatus.DISABLED);
        }
    }

    void setScale(int scale) {
        this.scale = scale;
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            ServiceInstanceImplementation instance = new ServiceInstanceImplementation(this, instanceNumber);
            instances.add(instance);
        }
    }

    private void evaluateExpression() {
        // TODO do it inside a sandbox container
        final String scriptName = "evaluateExpression_" + name;
        BashScript script = Utils.createScript(scriptName, logger);
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
                            log("Service interrupted when building/pulling image");
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
                logger.serviceDependencyInfo(dependency);
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
            log("Time limit has been reached");
            instances.forEach(ServiceInstanceImplementation::stop);
        }
    }

    private void setStatus(ServiceStatus status) {
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
        logger.serviceInfo(this);
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