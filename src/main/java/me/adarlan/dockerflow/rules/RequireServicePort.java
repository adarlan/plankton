package me.adarlan.dockerflow.rules;

import java.io.IOException;
import java.net.Socket;

import lombok.Getter;
import lombok.ToString;
import me.adarlan.dockerflow.Job;

@Getter
@ToString(of = { "name", "port", "ruleStatus" })
public class RequireServicePort implements Rule {

    private final Job parentJob;

    private final String name;

    private final Job requiredJob;

    private final String port;

    private RuleStatus ruleStatus = RuleStatus.WAITING;

    public RequireServicePort(Job parentJob, String name, Job requiredJob, String port) {
        this.parentJob = parentJob;
        this.name = name;
        this.requiredJob = requiredJob;
        this.port = port;
        log();
    }

    private void log() {
        System.out.println(parentJob.getName() + "::" + name + "=" + getValue() + " [" + ruleStatus + "]");
    }

    @Override
    public void updateStatus() {
        if (ruleStatus.equals(RuleStatus.WAITING)) {
            if (requiredJob.getFinalStatus() != null) {
                this.ruleStatus = RuleStatus.PASSED;
                log();
            } else {
                try (Socket s = new Socket(requiredJob.getName(), Integer.parseInt(port))) {
                    this.ruleStatus = RuleStatus.PASSED;
                    log();
                } catch (IOException ex) {
                    /* ignore */
                }
            }
        }
    }

    @Override
    public String getValue() {
        return port;
    }
}