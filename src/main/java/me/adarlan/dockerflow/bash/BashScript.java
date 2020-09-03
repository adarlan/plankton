package me.adarlan.dockerflow.bash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BashScript {

    private String name;

    private List<String> commands = new ArrayList<>();

    private List<String> variables = new ArrayList<>();

    private Process process;

    private Integer exitCode;

    private Consumer<String> forEachOutput;

    private Consumer<String> forEachError;

    public BashScript(String name) {
        this.name = name;
        this.commands.add("#!/bin/bash");
        this.commands.add("set -e");
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

    public BashScript run() throws InterruptedException {
        ProcessBuilder processBuilder = createProcessBuilder();
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new BashScriptException("Unable to start the process of script: " + name, e);
        }
        followOutput();
        followError();
        exitCode = process.waitFor();
        return this;
    }

    public int getExitCode() {
        if (exitCode == null) {
            throw new BashScriptException("The script was not run");
        }
        return exitCode;
    }

    private ProcessBuilder createProcessBuilder() {
        File tempScript;
        try {
            tempScript = File.createTempFile(name, null);
        } catch (final IOException e) {
            throw new BashScriptException("Unable to create the bash script file of script: " + name, e);
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
            throw new BashScriptException("Unable to create the process builder of script: " + name, e);
        }
    }

    private void followOutput() {
        if (forEachOutput != null) {
            new Thread(() -> {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        forEachOutput.accept(line);
                    }
                } catch (IOException e) {
                    throw new BashScriptException("Unable to follow the output stream of script: " + name, e);
                }
            }).start();
        }
    }

    private void followError() {
        if (forEachError != null) {
            new Thread(() -> {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                try {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        forEachError.accept(line);
                    }
                } catch (IOException e) {
                    throw new BashScriptException("Unable to follow the error stream of script: " + name, e);
                }
            }).start();
        }
    }
}