package me.adarlan.dockerflow.rules;

import java.io.File;

import lombok.Getter;
import lombok.ToString;
import me.adarlan.dockerflow.Job;

@Getter
@ToString(of = { "name", "filePath", "ruleStatus" })
public class RequireFile implements Rule {

    private final Job parentJob;

    private final String name;

    private final String filePath;

    private RuleStatus ruleStatus = RuleStatus.WAITING;

    public RequireFile(Job parentJob, String name, String filePath) {
        this.parentJob = parentJob;
        this.name = name;
        this.filePath = filePath;
        log();
    }

    @Override
    public String getValue() {
        return filePath;
    }

    @Override
    public Job getRequiredJob() {
        return null;
    }

    private void log() {
        System.out.println(parentJob.getName() + "::" + name + "=" + getValue() + " [" + ruleStatus + "]");
    }

    @Override
    public void updateStatus() {
        if (ruleStatus.equals(RuleStatus.WAITING)) {
            File file = new File(filePath);
            if (file.exists()) {
                ruleStatus = RuleStatus.PASSED;
                log();
            }
        }
    }
}