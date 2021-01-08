package me.adarlan.plankton.api.dependency;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.api.Job;
import me.adarlan.plankton.api.JobDependency;
import me.adarlan.plankton.api.JobDependencyStatus;
import me.adarlan.plankton.api.JobStatus;

@EqualsAndHashCode(of = { "parentJob", "requiredJob" })
public class WaitDependencyFailure implements JobDependency {

    @Getter
    Job parentJob;

    @Getter
    Job requiredJob;

    @Getter
    JobDependencyStatus status = JobDependencyStatus.WAITING;

    public WaitDependencyFailure(Job parentJob, Job requiredJob) {
        this.parentJob = parentJob;
        this.requiredJob = requiredJob;
    }

    @Override
    public Boolean updateStatus() {
        if (status.equals(JobDependencyStatus.WAITING) && requiredJob.hasEnded()) {
            if (requiredJob.getStatus().equals(JobStatus.FAILURE)) {
                status = JobDependencyStatus.PASSED;
                return true;
            } else {
                status = JobDependencyStatus.BLOCKED;
                return true;
            }
        }
        return false;
    }
}