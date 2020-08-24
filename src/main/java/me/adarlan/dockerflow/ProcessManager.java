package me.adarlan.dockerflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
public class ProcessManager {

    public ProcessManager() {
        //
    }

    public void startProcess(Job job) {
        final ProcessBuilder pb = processBuilder(job);
        try {
            job.process = pb.start();
        } catch (final IOException e1) {
            throw new DockerflowException(e1);
        }
    }

    @Async
    public void waitForExitCode(Job job) {
        try {
            job.exitCode = job.process.waitFor();
            //if (job.process.waitFor(5L, TimeUnit.SECONDS)) {
            //    job.exitCode = job.process.exitValue();
            //}else{
            //    job.setStatus(JobStatus.TIMEOUT);
            //}
        } catch (InterruptedException e) {
            job.setStatus(JobStatus.INTERRUPTED);
        }
    }

    private ProcessBuilder processBuilder(final Job job) {
        File tempScript;
        try {
            tempScript = File.createTempFile("containerUpScript", null);
        } catch (final IOException e) {
            throw new DockerflowException(e);
        }
        try (Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));) {
            final PrintWriter printWriter = new PrintWriter(streamWriter);
            printWriter.println("#!/bin/bash");
            printWriter.println("set -eu");
            printWriter.println("docker-compose --project-name " + job.getPipeline().getId()
                    + " up --force-recreate --abort-on-container-exit --exit-code-from " + job.getName() + " "
                    + job.getName());
            printWriter.close();
            final ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
            pb.inheritIO();
            pb.redirectOutput(new File(".dockerflow/logs/" + job.getName() + ".log"));
            pb.redirectError(new File(".dockerflow/logs/" + job.getName() + ".error.log"));
            return pb;
        } catch (final FileNotFoundException e) {
            throw new DockerflowException(e);
        } catch (final IOException e) {
            throw new DockerflowException(e);
        }
    }

    public void destroyProcess(final Job job) {
        job.process.destroy();
        // TODO aguardar o processo ser criado antes de destruir
    }
}