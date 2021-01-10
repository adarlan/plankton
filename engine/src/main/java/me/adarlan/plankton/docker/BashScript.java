package me.adarlan.plankton.docker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.Getter;
import me.adarlan.plankton.api.Logger;

public class BashScript {

    @Getter
    private String name;

    private List<String> variables = new ArrayList<>();
    private List<String> commands = new ArrayList<>();

    private Process process;
    private Integer exitCode;

    private Consumer<String> forEachOutput;
    private Consumer<String> forEachError;

    private final Logger logger = Logger.getLogger();

    public BashScript(String name) {
        this.name = name;
    }

    public BashScript command(String command) {
        this.commands.add(command);
        return this;
    }

    public BashScript commands(List<String> commands) {
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

    public BashScript forEachOutput(Consumer<String> forEachOutput) {
        this.forEachOutput = forEachOutput;
        return this;
    }

    public BashScript forEachError(Consumer<String> forEachError) {
        this.forEachError = forEachError;
        return this;
    }

    public BashScript forEachOutputAndError(Consumer<String> forEachOutputAndError) {
        this.forEachOutput = forEachOutputAndError;
        this.forEachError = forEachOutputAndError;
        return this;
    }

    public BashScript run() {
        variables.forEach(variable -> logger.debug(() -> name + " | " + variable));
        commands.forEach(command -> logger.debug(() -> name + " | " + command));
        commands.addAll(0, Arrays.asList("#!/bin/bash", "set -e"));
        ProcessBuilder processBuilder = createProcessBuilder();
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new PlanktonDockerException("Unable to start script: " + name, e);
        }
        Thread followOutput = followOutput();
        Thread followError = followError();
        followOutput.start();
        followError.start();
        try {
            exitCode = process.waitFor();
            followOutput.join();
            followError.join();
        } catch (InterruptedException e) {
            PlanktonDockerException exception = new PlanktonDockerException("Unable to run script: " + name, e);
            Thread.currentThread().interrupt();
            throw exception;
        }
        return this;
    }

    public int getExitCode() {
        if (exitCode == null) {
            throw new PlanktonDockerException("The script was not run");
        }
        return exitCode;
    }

    public void runSuccessfullyOrThrow(Supplier<RuntimeException> exceptionSupplier) {
        run();
        if (exitCode != 0) {
            throw exceptionSupplier.get();
        }
    }

    public void runSuccessfully() {
        runSuccessfullyOrThrow(() -> new PlanktonDockerException("Script failed: " + name));
    }

    private ProcessBuilder createProcessBuilder() {
        File tempScript;
        try {
            tempScript = File.createTempFile(name, null);
        } catch (final IOException e) {
            throw new PlanktonDockerException("Unable to create the bash script file of script: " + name, e);
        }
        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));) {
            final PrintWriter printWriter = new PrintWriter(streamWriter);
            commands.forEach(printWriter::println);
            printWriter.close();
            final ProcessBuilder processBuilder = new ProcessBuilder("bash", tempScript.toString());
            variables.forEach(keyValue -> {
                int separatorIndex = keyValue.indexOf("=");
                String key = keyValue.substring(0, separatorIndex).trim();
                String value = keyValue.substring(separatorIndex + 1).trim();
                processBuilder.environment().put(key, value);
            });
            return processBuilder;
        } catch (final IOException e) {
            throw new PlanktonDockerException("Unable to create the process builder of script: " + name, e);
        }
    }

    private Thread followOutput() {
        return new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (forEachOutput == null) {
                        final String l = line;
                        logger.debug(() -> name + " >> " + l);
                    } else {
                        forEachOutput.accept(line);
                    }
                }
            } catch (IOException e) {
                throw new PlanktonDockerException("Unable to follow the output stream of script: " + name, e);
            }
        });
    }

    private Thread followError() {
        return new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (forEachError == null) {
                        final String l = line;
                        logger.error(() -> name + " >> " + l);
                    } else {
                        forEachError.accept(line);
                    }
                }
            } catch (IOException e) {
                throw new PlanktonDockerException("Unable to follow the error stream of script: " + name, e);
            }
        });
    }
}