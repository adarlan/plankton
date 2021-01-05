package me.adarlan.dockerflow.data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class Job {

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
}
