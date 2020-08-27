package me.adarlan.dockerflow.rules;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.Job;
import me.adarlan.dockerflow.JobStatus;
import me.adarlan.dockerflow.Rule;
import me.adarlan.dockerflow.RuleDependency;
import me.adarlan.dockerflow.RuleStatus;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequireTaskStatus implements Rule, RuleDependency {

    @Getter
    Job parentJob;

    @Getter
    String name;

    @Getter
    Job requiredJob;

    @Getter
    JobStatus requiredStatus;

    @Getter
    RuleStatus status = RuleStatus.WAITING;

    public RequireTaskStatus(Job parentJob, String name, Job requiredJob, JobStatus requiredStatus) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
        this.requiredStatus = requiredStatus;
    }

    @Override
    public void updateStatus() {
        if (status.equals(RuleStatus.WAITING)) {
            JobStatus finalStatus = requiredJob.getFinalStatus();
            if (finalStatus != null) {
                if (finalStatus.equals(requiredStatus)) {
                    status = RuleStatus.PASSED;
                } else {
                    status = RuleStatus.BLOCKED;
                }
            }
        }
    }

    @Override
    public String getValue() {
        return requiredStatus.toString().toLowerCase();
    }
}