package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
                processManager.validateProcess(job);
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
            if (!rule.getStatus().equals(RuleStatus.PASSED)) {
                passed = false;
            }
        }
        return passed;
    }

    private void run(Job job) {
        processManager.startProcess(job);
        processManager.waitForExitCode(job);
        pipeline.setStatus(job, JobStatus.RUNNING);
    }

    private void cancel(Job job) {
        if (job.getStatus().equals(JobStatus.RUNNING)) {
            processManager.destroyProcess(job);
        }
        pipeline.setStatus(job, JobStatus.CANCELED);
    }

    private void log(Rule rule) {
        System.out.println(rule.getParentJob().getName() + "::" + rule.getName() + "::" + rule.getValue() + " -> "
                + rule.getStatus());
    }
}
