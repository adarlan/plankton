package me.adarlan.dockerflow.bash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
public class BashScriptRunner {

    public boolean runSuccessfully(BashScript script) {
        return runAndGetExitCode(script) == 0;
    }

    public int runAndGetExitCode(BashScript script) {
        run(script);
        return script.exitCode;
    }

    @Async
    public void runAsync(BashScript script) {
        run(script);
    }

    public void run(BashScript script) {
        startProcess(script);
        waitProcess(script);
        exit(script);
    }

    public void cancel(BashScript script) {
        if (script.process.isAlive()) {
            script.process.destroy();
        }
        if (script.process.isAlive()) {
            script.process.destroyForcibly();
        }
        if (script.process.isAlive()) {
            throw new BashScriptException("Unable to cancel the process");
        } else {
            script.exitedByCancel = true;
        }
    }

    private void startProcess(BashScript script) {
        if (script.process != null) {
            throw new BashScriptException("The process has already been started");
        }
        ProcessBuilder processBuilder = createProcessBuilder(script);
        try {
            script.process = processBuilder.start();
            if (script.onStart != null)
                script.onStart.run();
        } catch (IOException e) {
            throw new BashScriptException("Unable to start the process", e);
        }
    }

    private void waitProcess(BashScript script) {
        try {
            if (script.timeoutLimit == null) {
                script.exitCode = script.process.waitFor();
                if (script.exitCode.equals(0))
                    script.exitedBySuccess = true;
                else
                    script.exitedByFailure = true;
            } else if (script.process.waitFor(script.timeoutLimit, script.timeoutUnit)) {
                script.exitCode = script.process.exitValue();
                if (script.exitCode.equals(0))
                    script.exitedBySuccess = true;
                else
                    script.exitedByFailure = true;
            } else {
                script.exitedByTimeout = true;
            }
        } catch (InterruptedException e) {
            script.exitedByInterruption = true;
            Thread.currentThread().interrupt();
        }
    }

    private void exit(BashScript script) {
        if (script.onExit != null)
            script.onExit.run();
    }

    /*
     * private void exit(BashScript script) { if (script.exitedByInterruption)
     * runOnExit(script.onInterruption); else if (script.exitedBySuccess)
     * runOnExit(script.onSuccess); else if (script.exitedByTimeout)
     * runOnExit(script.onTimeout); else if (script.exitedByCancel)
     * runOnExit(script.onCancel); else if (script.exitedByFailure)
     * runOnExit(script.onFailure); }
     * 
     * private void runOnExit(Runnable runnable) { if (runnable != null)
     * runnable.run(); }
     */

    private ProcessBuilder createProcessBuilder(BashScript script) {
        File tempScript;
        try {
            tempScript = File.createTempFile(this.toString(), null);
        } catch (final IOException e) {
            throw new BashScriptException("Unable to create the temp file", e);
        }
        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));) {
            final PrintWriter printWriter = new PrintWriter(streamWriter);
            script.commands.forEach(printWriter::println);
            printWriter.close();
            final ProcessBuilder processBuilder = new ProcessBuilder("bash", tempScript.toString());
            script.variables.forEach(keyValue -> {
                int separatorIndex = keyValue.indexOf("=");
                String key = keyValue.substring(0, separatorIndex).trim();
                String value = keyValue.substring(separatorIndex + 1).trim();
                processBuilder.environment().put(key, value);
            });
            if (script.outputFilePath != null) {
                processBuilder.redirectOutput(new File(script.outputFilePath));
            } else {
                processBuilder.redirectOutput(Redirect.INHERIT);
            }
            if (script.errorFilePath != null) {
                processBuilder.redirectError(new File(script.errorFilePath));
            } else {
                processBuilder.redirectError(Redirect.INHERIT);
            }
            return processBuilder;
        } catch (final FileNotFoundException e) {
            throw new BashScriptException("Unable to create the temp file output stream", e);
        } catch (final IOException e) {
            throw new BashScriptException("Unable to close the temp file stream writer", e);
        }
    }
}