package me.adarlan.dockerflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@ToString(of = "name")
public class Job {

    @Getter
    String name;

    @Getter
    Integer scale;

    List<String> variables;

    @Getter
    String expression;

    Boolean expressionResult;

    @Getter
    Set<Rule> rules;

    @Getter
    Set<Job> allDependencies;

    @Getter
    Set<Job> directDependencies;

    @Getter
    Integer dependencyLevel;

    @Getter
    Set<Job> allDependents;

    @Getter
    Long timeout;

    @Getter
    TimeUnit timeoutUnit;

    public final List<String> logs = new ArrayList<>();

    public final State state = new State();

    public class State {

        @Getter
        private JobStatus status;

        @Getter
        private JobStatus finalStatus;

        @Getter
        int exitedCount = 0;

        private State() {
        }

        void increaseExitedCount() {
            exitedCount++;
        }

        void set(JobStatus status) {
            this.status = status;
            switch (status) {
                case WAITING:
                    break;
                case SCHEDULED:
                    break;
                case RUNNING:
                    break;
                case DISABLED:
                case BLOCKED:
                case INTERRUPTED:
                case CANCELED:
                case FAILED:
                case TIMEOUT:
                case FINISHED:
                    this.finalStatus = status;
            }
            Logger.info(() -> Job.this.name + ".status: " + this.status);
        }
    }

    @lombok.Data
    public static class Data {
        String name;
        Integer scale;
        List<String> variables = new ArrayList<>();
        String expression;
        Boolean expressionResult;
        String timeout;
        String status;
        String finalStatus;
        List<Rule.Data> rules = new ArrayList<>();
        List<String> allDependents = new ArrayList<>();
        List<String> allDependencies = new ArrayList<>();
        List<String> directDependencies = new ArrayList<>();
        Integer dependencyLevel;
    }

    public Data getData() {
        Data data = new Data();
        data.name = this.name;
        data.scale = this.scale;
        this.variables.forEach(data.variables::add);
        data.expression = this.expression;
        data.expressionResult = this.expressionResult;
        data.timeout = this.timeout.toString() + this.timeoutUnit.toString().substring(0, 1).toLowerCase();
        data.status = this.state.status.toString().toLowerCase();
        data.finalStatus = this.state.finalStatus == null ? null : this.state.finalStatus.toString().toLowerCase();
        this.rules.forEach(rule -> data.rules.add(rule.getData()));
        this.allDependents.forEach(d -> data.allDependents.add(d.name));
        this.allDependencies.forEach(d -> data.allDependencies.add(d.name));
        this.directDependencies.forEach(d -> data.directDependencies.add(d.name));
        data.dependencyLevel = this.dependencyLevel;
        return data;
    }
}