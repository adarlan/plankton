package me.adarlan.plankton.pipeline.dependencies;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.pipeline.Job;
import me.adarlan.plankton.pipeline.JobDependency;
import me.adarlan.plankton.pipeline.JobStatus;

@EqualsAndHashCode
@ToString(of = "requiredJob")
public class WaitFailureOf implements JobDependency {

    Job parentJob;

    Job requiredJob;

    public WaitFailureOf(Job parentJob, Job requiredJob) {
        this.parentJob = parentJob;
        this.requiredJob = requiredJob;
    }

    @Override
    public boolean isSatisfied() {
        return requiredJob.status().isFailed();
    }

    @Override
    public boolean isBlocked() {
        JobStatus status = requiredJob.status();
        return !(status.isWaiting() || status.isRunning() || status.isFailed());
    }

    @Override
    public Job job() {
        return requiredJob;
    }
}
