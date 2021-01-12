package me.adarlan.plankton.util;

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

public class BashScript {

    @Getter
    private String name;

    private List<String> variables = new ArrayList<>();
    private List<String> commands = new ArrayList<>();

    private Process process;
    private Integer exitCode;

    private Consumer<String> forEachVariable;
    private Consumer<String> forEachCommand;

    private Consumer<String> forEachOutput;
    private Consumer<String> forEachError;

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

    public BashScript forEachVariable(Consumer<String> forEachVariable) {
        this.forEachVariable = forEachVariable;
        return this;
    }

    public BashScript forEachCommand(Consumer<String> forEachCommand) {
        this.forEachCommand = forEachCommand;
        return this;
    }

    public BashScript forEachVariableAndCommand(Consumer<String> forEachVariableAndCommand) {
        this.forEachVariable = forEachVariableAndCommand;
        this.forEachCommand = forEachVariableAndCommand;
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
        if (forEachVariable != null)
            variables.forEach(forEachVariable::accept);
        if (forEachCommand != null)
            commands.forEach(forEachCommand::accept);
        commands.addAll(0, Arrays.asList("#!/bin/bash", "set -e"));
        ProcessBuilder processBuilder = createProcessBuilder();
        process = startProcessBuilder(processBuilder);
        Thread outputStreamThread = followOutputStream();
        Thread errorStreamThread = followErrorStream();
        outputStreamThread.start();
        errorStreamThread.start();
        exitCode = waitForProcess();
        joinOutputStreamThread(outputStreamThread);
        joinErrorStreamThread(errorStreamThread);
        return this;
    }

    private int waitForProcess() {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            BashScriptException exception = new BashScriptException(this, "Unable to wait for process", e);
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void joinOutputStreamThread(Thread outputStreamThread) {
        try {
            outputStreamThread.join();
        } catch (InterruptedException e) {
            BashScriptException exception = new BashScriptException(this, "Unable join output stream thread", e);
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private void joinErrorStreamThread(Thread errorStreamThread) {
        try {
            errorStreamThread.join();
        } catch (InterruptedException e) {
            BashScriptException exception = new BashScriptException(this, "Unable join error stream thread", e);
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    public int getExitCode() {
        if (exitCode == null) {
            throw new BashScriptException(this, "Unable to get exit code; The script was not run");
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
        runSuccessfullyOrThrow(() -> new BashScriptException(this, "Unable to run successfully"));
        // TODO BashScriptFailedException extends BashScriptException
    }

    private ProcessBuilder createProcessBuilder() {
        File tempScript = createTempFile();
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
            throw new BashScriptException(this, "Unable to create process builder", e);
        }
    }

    private Process startProcessBuilder(ProcessBuilder processBuilder) {
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new BashScriptException(this, "Unable to start process builder", e);
        }
    }

    private File createTempFile() {
        try {
            return File.createTempFile(name, null);
        } catch (final IOException e) {
            throw new BashScriptException(this, "Unable to create temp file", e);
        }
    }

    private Thread followOutputStream() {
        return new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    forEachOutput.accept(line);
                }
            } catch (IOException e) {
                throw new BashScriptException(this, "Unable to follow output stream", e);
            }
        });
    }

    private Thread followErrorStream() {
        return new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    forEachError.accept(line);
                }
            } catch (IOException e) {
                throw new BashScriptException(this, "Unable to follow error stream", e);
            }
        });
    }
}