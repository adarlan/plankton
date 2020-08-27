package me.adarlan.dockerflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
public class ProcessManager {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private Pipeline pipeline;

    private Map<Job, Process> processByJob = new HashMap<>();

    public ProcessManager() {
        //
    }

    public void startProcess(Job job) {
        final ProcessBuilder pb = processBuilder(job);
        try {
            Process process = pb.start();
            processByJob.put(job, process);
        } catch (final IOException e1) {
            throw new DockerflowException(e1);
        }
    }

    @Async
    public void waitForExitCode(Job job) {
        try {
            Process process = processByJob.get(job);
            job.exitCode = process.waitFor();
            // if (job.process.waitFor(5L, TimeUnit.SECONDS)) {
            // job.exitCode = job.process.exitValue();
            // }else{
            // job.setStatus(JobStatus.TIMEOUT);
            // }
        } catch (InterruptedException e) {
            pipeline.setStatus(job, JobStatus.INTERRUPTED);
        }
    }

    public void validateProcess(Job job) {
        Process process = processByJob.get(job);
        if (!process.isAlive()) {
            if (job.exitCode.equals(0)) {
                pipeline.setStatus(job, JobStatus.FINISHED);
            } else {
                pipeline.setStatus(job, JobStatus.FAILED);
            }
        }
    }

    @EventListener
    public void contextClosedEvent(ContextClosedEvent event) {
        System.out.println(
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "
                        + event.getClass().getName() + " >>> " + event);
    }

    @EventListener
    public void contextStoppedEvent(ContextStoppedEvent event) {
        System.out.println(
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "
                        + event.getClass().getName() + " >>> " + event);
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
            // TODO concatenar arquivo environment
            printWriter.println("docker-compose --project-name " + applicationConfig.getName() + " --file "
                    + applicationConfig.getFile() + " --project-directory " + applicationConfig.getWorkspace()
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
        Process process = processByJob.get(job);
        process.destroy();
        // TODO aguardar o processo ser criado antes de destruir
    }
}