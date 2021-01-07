package me.adarlan.plankton.serializable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.adarlan.plankton.Job;

@Getter
@Setter(AccessLevel.PACKAGE)
public class SerializableJob {

    String name;
    String expression;
    Boolean expressionResult;
    List<Object> rules;
    Integer scale;
    List<Object> instances;
    Instant initialInstant;
    Instant finalInstant;
    Duration duration;
    Duration timeout;
    List<String> logs;
    String status;

    Integer dependencyLevel;

    static SerializableJob of(Job job) {
        SerializableJob serializableJob = new SerializableJob();
        serializableJob.name = job.getName();
        serializableJob.expression = job.getExpression();
        serializableJob.expressionResult = job.getExpressionResult();
        serializableJob.rules = new ArrayList<>();
        serializableJob.scale = job.getScale();
        serializableJob.instances = new ArrayList<>();
        serializableJob.initialInstant = job.getInitialInstant();
        serializableJob.finalInstant = job.getFinalInstant();
        serializableJob.duration = job.getDuration();
        serializableJob.timeout = job.getTimeout();
        serializableJob.logs = new ArrayList<>();
        serializableJob.status = job.getStatus().toString().toLowerCase();
        return serializableJob;
    }

}
