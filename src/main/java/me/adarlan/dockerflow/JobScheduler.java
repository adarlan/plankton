package me.adarlan.dockerflow;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import me.adarlan.dockerflow.bash.BashScript;
import me.adarlan.dockerflow.bash.BashScriptRunner;
import me.adarlan.dockerflow.compose.DockerCompose;

@Service
@EnableScheduling
public class JobScheduler {

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private DockerCompose dockerCompose;

    @Autowired
    private BashScriptRunner bashScriptRunner;

    private Map<Job, BashScript> scriptsByJob = new HashMap<>();
    private Map<Job, BashScript> exitedScripts = new HashMap<>();
    private Set<Job> jobsWithFinalStatus = new HashSet<>();

    private boolean pipelineIsDone = false;
    private boolean allProcessesExited = false;
    private boolean showProcessesAlive = false;

    @Scheduled(fixedRate = 1000)
    public void scheduleJobs() {
        exitedScripts.forEach(this::treatExitedScript);
        pipeline.getJobsByStatus(JobStatus.WAITING).forEach(job -> {
            boolean passed = true;
            boolean blocked = false;
            for (final Rule rule : job.getRules()) {
                if (rule.updateStatus()) {
                    log(rule);
                }
                if (!rule.getStatus().equals(RuleStatus.PASSED))
                    passed = false;
                if (rule.getStatus().equals(RuleStatus.BLOCKED))
                    blocked = true;
            }
            if (passed) {
                scheduleToRunAsync(job);
            } else if (blocked) {
                job.status = JobStatus.BLOCKED;
                job.finalStatus = job.status;
                log(job);
            }
        });
        checkIfPipelineIsDone();
        checkIfAllProcessesExited();
    }

    private void scheduleToRunAsync(final Job job) {
        job.status = JobStatus.SCHEDULED;
        log(job);
        final BashScript script = dockerCompose.getServiceUpBashScript(job.name, 1);
        scriptsByJob.put(job, script);
        script.timeout(job.timeout, job.timeoutUnit);
        script.onStart(() -> {
            job.initialInstant = Instant.now();
            job.status = JobStatus.RUNNING;
            log(job);
        });
        script.onExit(() -> exitedScripts.put(job, script));
        bashScriptRunner.runAsync(script);
    }

    public void cancel(Job job) {
        switch (job.status) {
            case WAITING:
                job.status = JobStatus.CANCELED;
                job.finalStatus = job.status;
                log(job);
                break;
            case RUNNING:
                job.finalInstant = Instant.now();
                job.status = JobStatus.CANCELED;
                job.finalStatus = job.status;
                jobsWithFinalStatus.add(job);
                log(job);
                dockerCompose.stop(job.name);
                break;
            default:
                break;
        }
    }

    private void treatExitedScript(Job job, BashScript script) {
        if (jobsWithFinalStatus.contains(job)) {
            return;
        }
        if (script.exitedBySuccess) {
            job.finalInstant = Instant.now();
            job.exitCode = script.getExitCode();
            job.status = JobStatus.FINISHED;
            job.finalStatus = job.status;
            log(job);
        } else if (script.exitedByTimeout) {
            job.finalInstant = Instant.now();
            job.status = JobStatus.TIMEOUT;
            job.finalStatus = job.status;
            log(job);
            dockerCompose.stop(job.name);
        } else if (script.exitedByInterruption) {
            job.finalInstant = Instant.now();
            job.status = JobStatus.INTERRUPTED;
            job.finalStatus = job.status;
            log(job);
        } else if (script.exitedByFailure) {
            job.finalInstant = Instant.now();
            job.exitCode = script.getExitCode();
            job.status = JobStatus.FAILED;
            job.finalStatus = job.status;
            log(job);
        }
        jobsWithFinalStatus.add(job);
    }

    private void log(Job job) {
        System.out.println(logPrefix() + job.name + " => " + job.status);
    }

    private void log(Rule rule) {
        Job job = rule.getParentJob();
        System.out.println(
                logPrefix() + job.name + " => " + rule.getName() + "(" + rule.getValue() + ") => " + rule.getStatus());
    }

    private void checkIfPipelineIsDone() {
        if (!pipelineIsDone && pipeline.isDone()) {
            pipelineIsDone = true;
            System.out.println(logPrefix() + "All jobs have a final status!");
            showProcessesAlive = true;
        }
    }

    private void checkIfAllProcessesExited() {
        if (pipelineIsDone && !allProcessesExited) {
            Flag b = new Flag(true);
            scriptsByJob.forEach((job, script) -> {
                if (script.process.isAlive()) {
                    b.set(false);
                    if (showProcessesAlive) {
                        System.out.println(logPrefix() + "The process for " + job.name + " is still alive");
                    }
                }
            });
            if (b.get()) {
                allProcessesExited = true;
                System.out.println(logPrefix() + "All processes have exited!");
            }
        }
    }

    private String logPrefix() {
        return Instant.now() + " " + this.getClass().getSimpleName() + " ";
    }

    private static class Flag {
        boolean value;

        Flag(boolean value) {
            this.value = value;
        }

        void set(boolean value) {
            this.value = value;
        }

        boolean get() {
            return value;
        }
    }
}
