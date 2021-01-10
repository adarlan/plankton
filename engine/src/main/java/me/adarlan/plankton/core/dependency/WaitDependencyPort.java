package me.adarlan.plankton.core.dependency;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.core.Job;
import me.adarlan.plankton.core.JobDependency;
import me.adarlan.plankton.core.JobDependencyStatus;
import me.adarlan.plankton.core.JobStatus;

@EqualsAndHashCode(of = { "parentJob", "requiredJob", "port" })
public class WaitDependencyPort implements JobDependency {

    @Getter
    Job parentJob;

    @Getter
    Job requiredJob;

    @Getter
    Integer port;

    @Getter
    JobDependencyStatus status = JobDependencyStatus.WAITING;

    public WaitDependencyPort(Job parentJob, Job requiredJob, Integer port) {
        this.parentJob = parentJob;
        this.requiredJob = requiredJob;
        this.port = port;
    }

    @Override
    public Boolean updateStatus() {
        if (status.equals(JobDependencyStatus.WAITING)) {
            if (requiredJob.hasEnded()) {
                status = JobDependencyStatus.BLOCKED;
                return true;
            } else if (requiredJob.getStatus().equals(JobStatus.RUNNING)) {
                try (Socket s = new Socket("localhost", port)) {
                    status = JobDependencyStatus.PASSED;
                    return true;
                } catch (IOException ex) {
                    /* ignore */
                }
            }
        }
        return false;
    }
}
