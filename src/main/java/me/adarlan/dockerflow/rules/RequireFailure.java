package me.adarlan.dockerflow.rules;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.Job;
import me.adarlan.dockerflow.JobStatus;
import me.adarlan.dockerflow.Rule;
import me.adarlan.dockerflow.RuleWithDependency;
import me.adarlan.dockerflow.RuleStatus;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequireFailure implements Rule, RuleWithDependency {

    @Getter
    Job parentJob;

    @Getter
    String name;

    @Getter
    Job requiredJob;

    @Getter
    RuleStatus status = RuleStatus.WAITING;

    public RequireFailure(Job parentJob, String name, Job requiredJob) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
    }

    @Override
    public boolean updateStatus() {
        if (status.equals(RuleStatus.WAITING)) {
            JobStatus finalStatus = requiredJob.getFinalStatus();
            if (finalStatus != null) {
                if (finalStatus.equals(JobStatus.FINISHED)) {
                    status = RuleStatus.BLOCKED;
                    return true;
                } else {
                    status = RuleStatus.PASSED;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getValue() {
        return requiredJob.getName();
    }
}