package me.adarlan.plankton.rules;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.Job;
import me.adarlan.plankton.JobStatus;
import me.adarlan.plankton.Rule;
import me.adarlan.plankton.RuleStatus;
import me.adarlan.plankton.RuleWithDependency;

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
        if (status.equals(RuleStatus.WAITING) && requiredJob.hasEnded()) {
            if (requiredJob.getStatus().equals(JobStatus.FAILURE)) {
                status = RuleStatus.PASSED;
                return true;
            } else {
                status = RuleStatus.BLOCKED;
                return true;
            }
        }
        return false;
    }

    @Override
    public String getValue() {
        return requiredJob.getName();
    }
}