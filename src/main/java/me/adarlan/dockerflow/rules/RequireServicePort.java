package me.adarlan.dockerflow.rules;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.Job;
import me.adarlan.dockerflow.Rule;
import me.adarlan.dockerflow.RuleDependency;
import me.adarlan.dockerflow.RuleStatus;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequireServicePort implements Rule, RuleDependency {

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

    public RequireServicePort(Job parentJob, String name, Job requiredJob, Integer port) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
        this.port = port;
    }

    @Override
    public void updateStatus() {
        if (status.equals(RuleStatus.WAITING)) {
            if (requiredJob.getFinalStatus() != null) {
                status = RuleStatus.BLOCKED;
            } else {
                try (Socket s = new Socket(requiredJob.getName(), port)) {
                    status = RuleStatus.PASSED;
                } catch (IOException ex) {
                    /* ignore */
                }
            }
        }
    }

    @Override
    public Integer getValue() {
        return port;
    }
}