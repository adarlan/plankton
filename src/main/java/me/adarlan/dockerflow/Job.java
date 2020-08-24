package me.adarlan.dockerflow;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.ComponentScan;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.rules.Rule;

@ComponentScan
@EqualsAndHashCode(of = "name")
public class Job {

    @Getter
    private final Pipeline pipeline;

    @Getter
    private final String name;

    final Map<String, Object> data;

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
    Set<Rule> rules;

    Process process = null;

    @Getter
    Integer exitCode = null;

    @Getter
    Set<Job> allDependencies;

    @Getter
    Set<Job> directDependencies;

    @Getter
    Integer dependencyLevel;

    @Getter
    Set<Job> allDependents = new HashSet<>();

    Job(final Pipeline pipeline, final String name, final Map<String, Object> data) {
        this.pipeline = pipeline;
        this.name = name;
        this.data = data;
    }

    void setStatus(final JobStatus status) {
        this.status = status;
        switch (status) {
            case INTERRUPTED:
            case CANCELED:
            case FAILED:
            case TIMEOUT:
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