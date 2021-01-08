package me.adarlan.plankton.rules;

import java.io.File;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.docker.Job;
import me.adarlan.plankton.docker.Rule;
import me.adarlan.plankton.docker.RuleStatus;

@EqualsAndHashCode(of = { "parentJob", "name" })
public class RequireFile implements Rule {

    @Getter
    Job parentJob;

    @Getter
    String name;

    @Getter
    String filePath;

    @Getter
    RuleStatus status = RuleStatus.WAITING;

    public RequireFile(Job parentJob, String name, String filePath) {
        this.parentJob = parentJob;
        this.name = name;
        this.filePath = filePath;
    }

    @Override
    public boolean updateStatus() {
        if (status.equals(RuleStatus.WAITING)) {
            // TODO BLOCKED se todos os demais jobs estiverem finalizados
            File file = new File(filePath);
            if (file.exists()) {
                status = RuleStatus.PASSED;
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getValue() {
        return filePath;
    }
}