package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import me.adarlan.dockerflow.rules.Rule;
import me.adarlan.dockerflow.rules.RuleStatus;

@Service
@EnableScheduling
public class JobScheduler {

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private ProcessManager processManager;

    @Scheduled(fixedRate = 1000)
    public void scheduleJobs() {
        pipeline.getJobs().forEach(this::scheduleJob);
    }

    private void scheduleJob(Job job) {
        switch (job.getStatus()) {
            case WAITING: {
                if (isRulesPassed(job)) {
                    run(job);
                }
                break;
            }
            case RUNNING: {
                if (job.process != null && !job.process.isAlive()) {
                    if (job.exitCode.equals(0)) {
                        job.setStatus(JobStatus.FINISHED);
                    } else {
                        job.setStatus(JobStatus.FAILED);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private boolean isRulesPassed(Job job) {
        boolean passed = true;
        for (final Rule rule : job.getRules()) {
            rule.updateStatus();
            if (!rule.getRuleStatus().equals(RuleStatus.PASSED)) {
                passed = false;
            }
        }
        return passed;
    }

    private void run(Job job) {
        job.setStatus(JobStatus.RUNNING);
        processManager.startProcess(job);
        processManager.waitForExitCode(job);
    }

    private void cancel(Job job) {
        job.setStatus(JobStatus.CANCELED);
        if (job.getStatus().equals(JobStatus.RUNNING)) {
            processManager.destroyProcess(job);
        }
    }
}
