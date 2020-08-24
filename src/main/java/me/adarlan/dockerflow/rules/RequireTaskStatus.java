package me.adarlan.dockerflow.rules;

import lombok.Getter;
import lombok.ToString;
import me.adarlan.dockerflow.Job;
import me.adarlan.dockerflow.JobStatus;

@Getter
@ToString(of = { "name", "requiredStatus", "ruleStatus" })
public class RequireTaskStatus implements Rule {

    private final Job parentJob;

    private final String name;

    private final Job requiredJob;

    private final JobStatus requiredStatus;

    private RuleStatus ruleStatus = RuleStatus.WAITING;

    public RequireTaskStatus(Job parentJob, String name, Job requiredJob, JobStatus requiredStatus) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
        this.requiredStatus = requiredStatus;
        log();
    }

    @Override
    public String getValue() {
        return requiredStatus.toString().toLowerCase();
    }

    private void log() {
        System.out.println(parentJob.getName() + "::" + name + "=" + getValue() + " [" + ruleStatus + "]");
    }

    @Override
    public void updateStatus() {
        if (ruleStatus.equals(RuleStatus.WAITING)) {
            JobStatus finalStatus = requiredJob.getFinalStatus();
            if (finalStatus != null) {
                if (finalStatus.equals(requiredStatus)) {
                    ruleStatus = RuleStatus.PASSED;
                    log();
                } else {
                    ruleStatus = RuleStatus.BLOCKED;
                    log();
                }
            }
        }
    }
}