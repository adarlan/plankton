package me.adarlan.dockerflow;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.ComponentScan;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.rules.Rule;
import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequireServicePort;
import me.adarlan.dockerflow.rules.RequireTaskStatus;

@ComponentScan
@EqualsAndHashCode(of = "name")
public class Job {

    @Getter
    private final Pipeline pipeline;

    @Getter
    private final String name;

    private final Map<String, Object> data;

    @Getter
    private JobStatus status = JobStatus.WAITING;

    @Getter
    private JobStatus finalStatus = null;

    // @Getter
    // private Instant runInstant = null;
    // talvez o process já tenha essas informações
    // @Getter
    // private Instant finalInstant = null;

    @Getter
    private Set<Rule> rules;

    Process process = null;

    @Getter
    Integer exitCode = null;

    @Getter
    private Set<Job> allDependencies = new HashSet<>();

    @Getter
    private Set<Job> directDependencies = new HashSet<>();

    @Getter
    private Integer dependencyLevel = null;

    Job(final Pipeline pipeline, final String name, final Map<String, Object> data) {
        this.pipeline = pipeline;
        this.name = name;
        this.data = data;
    }

    void initialize() {
        initializeRules();
        initializeDependencies(new HashSet<>());
        log();
    }

    private void initializeRules() {
        rules = new HashSet<>();
        Map<String, Object> labels = Utils.getPropertyMap(this.data, "labels");
        labels.forEach((ruleName, value) -> {
            final String[] ss = ruleName.split("\\.");
            if (ss[0].equals("dockerflow")) {
                // TODO usar regex
                if (ss[1].equals("require")) {
                    if (ss[2].equals("service")) {
                        final Job targetJob = pipeline.getJobByName(ss[3]);
                        final String port = (String) labels.get(ruleName);
                        final Rule rule = new RequireServicePort(this, ruleName, targetJob, port);
                        rules.add(rule);
                    } else if (ss[2].equals("task")) {
                        final Job targetJob = pipeline.getJobByName(ss[3]);
                        final String statusString = (String) labels.get(ruleName);
                        final JobStatus jobStatus = JobStatus.valueOf(statusString.toUpperCase());
                        final Rule rule = new RequireTaskStatus(this, ruleName, targetJob, jobStatus);
                        rules.add(rule);
                    } else if (ss[2].equals("file")) {
                        final String filePath = (String) labels.get(ruleName);
                        final Rule rule = new RequireFile(this, ruleName, filePath);
                        rules.add(rule);
                    }
                }
            }
        });
    }

    private int initializeDependencies(Set<Job> knownDependents) {
        if (dependencyLevel == null) {
            knownDependents.add(this);
            int maxDepth = -1;
            for (Rule rule : getRules()) {
                Job dependency = rule.getRequiredJob();
                if (dependency != null) {
                    if (knownDependents.contains(dependency)) {
                        throw new DockerflowException("Dependency loop");
                    }
                    int d = dependency.initializeDependencies(knownDependents);
                    if (d > maxDepth)
                        maxDepth = d;
                    allDependencies.add(dependency);
                    allDependencies.addAll(dependency.allDependencies);
                    directDependencies.add(dependency);
                }
            }
            dependencyLevel = maxDepth + 1;
        }
        return dependencyLevel;
    }

    void setStatus(final JobStatus status) {
        this.status = status;
        switch (status) {
            case INTERRUPTED:
            case CANCELED:
            case FAILED:
            case FINISHED:
                this.finalStatus = status;
                break;
            default:
                break;
        }
        log();
    }

    private void log() {
        System.out.println(name + " " + status.toString().toLowerCase());
    }

    @Override
    public String toString() {
        return name + ": " + status;
    }
}