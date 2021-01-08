package me.adarlan.plankton.rules;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.api.JobStatus;
import me.adarlan.plankton.docker.Job;
import me.adarlan.plankton.docker.Rule;
import me.adarlan.plankton.docker.RuleStatus;
import me.adarlan.plankton.docker.RuleWithDependency;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequireSuccess implements Rule, RuleWithDependency {

    @Getter
    Job parentJob;

    @Getter
    String name;

    @Getter
    Job requiredJob;

    @Getter
    RuleStatus status = RuleStatus.WAITING;

    public RequireSuccess(Job parentJob, String name, Job requiredJob) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
    }

    @Override
    public boolean updateStatus() {
        if (status.equals(RuleStatus.WAITING) && requiredJob.hasEnded()) {
            if (requiredJob.getStatus().equals(JobStatus.SUCCESS)) {
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