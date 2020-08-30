package me.adarlan.dockerflow.bash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

public class BashScript {

    List<String> commands = new ArrayList<>();

    List<String> variables = new ArrayList<>();

    String outputFilePath;

    String errorFilePath;

    Long timeoutLimit;

    TimeUnit timeoutUnit;

    public Process process;

    @Getter
    Integer exitCode;

    Runnable onStart;

    Runnable onCancel;

    Runnable onSuccess;

    Runnable onFailure;

    Runnable onTimeout;

    Runnable onInterruption;

    Runnable onExit;

    boolean cancel = false;

    boolean exited = false;

    public boolean exitedBySuccess = false;

    boolean exitedByCancel = false;

    public boolean exitedByFailure = false;

    public boolean exitedByTimeout = false;

    public boolean exitedByInterruption = false;

    public BashScript() {
        this.commands.add("#!/bin/bash");
        this.commands.add("set -e");
    }

    public BashScript(String command) {
        this();
        this.commands.add(command);
    }

    public BashScript add(String command) {
        this.commands.add(command);
        return this;
    }

    public BashScript add(List<String> commands) {
        this.commands.addAll(commands);
        return this;
    }

    public BashScript env(String variable) {
        this.variables.add(variable);
        return this;
    }

    public BashScript env(List<String> variables) {
        this.variables.addAll(variables);
        return this;
    }

    public BashScript outputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
        return this;
    }

    public BashScript errorFilePath(String errorFilePath) {
        this.errorFilePath = errorFilePath;
        return this;
    }

    public BashScript timeout(Long limit, TimeUnit unit) {
        this.timeoutLimit = limit;
        this.timeoutUnit = unit;
        return this;
    }

    public BashScript onStart(Runnable onStart) {
        this.onStart = onStart;
        return this;
    }

    public BashScript onCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public BashScript onSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public BashScript onFailure(Runnable onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    public BashScript onTimeout(Runnable onTimeout) {
        this.onTimeout = onTimeout;
        return this;
    }

    public BashScript onInterruption(Runnable onInterruption) {
        this.onInterruption = onInterruption;
        return this;
    }

    public BashScript onExit(Runnable onExit) {
        this.onExit = onExit;
        return this;
    }
}