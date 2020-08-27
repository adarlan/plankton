package me.adarlan.dockerflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@EnableAsync
public class JobScheduler {

    @Autowired
    private DockerflowConfig dockerflowConfig;

    @Autowired
    private Pipeline pipeline;

    private Map<Job, Process> processByJob = new HashMap<>();

    @Scheduled(fixedRate = 1000)
    public void scheduleJobs() {
        pipeline.getJobsByStatus(JobStatus.WAITING).forEach(job -> {
            if (isRulesPassed(job)) {
                runJob(job);
            }
        });
    }

    private boolean isRulesPassed(Job job) {
        boolean passed = true;
        for (final Rule rule : job.getRules()) {
            rule.updateStatus();
            if (!rule.getStatus().equals(RuleStatus.PASSED)) {
                passed = false;
            }
        }
        return passed;
    }

    @Async
    public void runJob(Job job) {
        final ProcessBuilder processBuilder = processBuilder(job);
        Process process;
        try {
            job.status = JobStatus.RUNNING;
            process = processBuilder.start();
            job.initialInstant = Instant.now();
        } catch (final IOException e1) {
            throw new DockerflowException(e1);
        }
        processByJob.put(job, process);
        try {
            if (job.timeout != null) {
                if (process.waitFor(job.timeout, job.timeoutUnit)) {
                    job.finalInstant = Instant.now();
                    job.exitCode = process.exitValue();
                    if (job.exitCode.equals(0)) {
                        job.status = JobStatus.FINISHED;
                    } else {
                        job.status = JobStatus.FAILED;
                    }
                } else {
                    job.finalInstant = Instant.now();
                    job.status = JobStatus.TIMEOUT;
                }
            }
        } catch (InterruptedException e) {
            job.finalInstant = Instant.now();
            job.status = JobStatus.INTERRUPTED;
        }
    }

    public void cancelJob(Job job) {
        if (job.status.equals(JobStatus.RUNNING)) {
            Process process = processByJob.get(job);
            process.destroy();
            // TODO destroyForcibly?
            // TODO isso pode levar tempo... verificar process.isAlive?
        }
        job.status = JobStatus.CANCELED;
        job.finalInstant = Instant.now();
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
            printWriter.println("docker-compose --project-name " + dockerflowConfig.getName() + " --file "
                    + dockerflowConfig.getFile() + " --project-directory " + dockerflowConfig.getWorkspace()
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
}
