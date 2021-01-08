package me.adarlan.plankton.rules;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.api.JobStatus;
import me.adarlan.plankton.docker.Job;
import me.adarlan.plankton.docker.Rule;
import me.adarlan.plankton.docker.RuleStatus;
import me.adarlan.plankton.docker.RuleWithDependency;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequirePort implements Rule, RuleWithDependency {

    @Getter
    Job parentJob;

    @Getter
    String name;

    @Getter
    Job requiredJob;

    @Getter
    Integer port;

    @Getter
    RuleStatus status = RuleStatus.WAITING;

    public RequirePort(Job parentJob, String name, Job requiredJob, Integer port) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
        this.port = port;
    }

    @Override
    public boolean updateStatus() {
        if (status.equals(RuleStatus.WAITING)) {
            if (requiredJob.hasEnded()) {
                status = RuleStatus.BLOCKED;
                return true;
            } else if (requiredJob.getStatus().equals(JobStatus.RUNNING)) {
                try (Socket s = new Socket("localhost", port)) {
                    status = RuleStatus.PASSED;
                    return true;
                } catch (IOException ex) {
                    /* ignore */
                }
            }
        }
        return false;
    }

    @Override
    public Integer getValue() {
        return port;
    }
}