package me.adarlan.dockerflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@ToString
public class Job {

    @Getter
    String name;

    @Getter
    Long timeout;

    @Getter
    TimeUnit timeoutUnit;

    @Getter
    String expression;

    @Getter
    Set<Rule> rules;

    @Getter
    Set<Job> allDependencies;

    @Getter
    Set<Job> directDependencies;

    @Getter
    Integer dependencyLevel;

    @Getter
    Set<Job> allDependents = new HashSet<>();

    @Getter
    JobStatus status;

    @Getter
    JobStatus finalStatus;

    @Getter
    Instant initialInstant;

    @Getter
    Instant finalInstant;

    @Getter
    Integer exitCode;

    @lombok.Data
    public static class Data {
        String name;
        String timeout;
        String expression;
        String status;
        String finalStatus;
        Instant initialInstant;
        Instant finalInstant;
        Integer exitCode;
        List<Rule.Data> rules = new ArrayList<>();
        List<String> allDependents = new ArrayList<>();
        List<String> allDependencies = new ArrayList<>();
        List<String> directDependencies = new ArrayList<>();
        Integer dependencyLevel;
    }

    public Data getData() {
        Data data = new Data();
        data.name = this.name;
        data.timeout = this.timeout.toString() + this.timeoutUnit.toString().substring(0, 1).toLowerCase();
        data.expression = this.expression;
        data.status = this.status.toString().toLowerCase();
        data.finalStatus = this.finalStatus == null ? null : this.finalStatus.toString().toLowerCase();
        data.initialInstant = this.initialInstant;
        data.finalInstant = this.finalInstant;
        data.exitCode = this.exitCode;
        this.rules.forEach(rule -> data.rules.add(rule.getData()));
        this.allDependencies.forEach(d -> data.allDependencies.add(d.name));
        this.directDependencies.forEach(d -> data.directDependencies.add(d.name));
        this.allDependents.forEach(d -> data.allDependents.add(d.name));
        data.dependencyLevel = this.dependencyLevel;
        return data;
    }
}