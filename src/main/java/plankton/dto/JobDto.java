package plankton.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import plankton.core.Job;

@Data
public class JobDto {

    String name;
    String status;
    Map<String, String> dependencies = new HashMap<>();

    public JobDto(Job job) {
        this.name = job.name();
        this.status = statusOf(job);
        job.dependencyMap().forEach((j, c) -> this.dependencies.put(j.name(), c.toString().toLowerCase()));
    }

    private String statusOf(Job job) {
        switch (job.status()) {
            case WAITING:
                return "waiting";
            case PULLING:
            case BUILDING:
            case RUNNING:
                return "running";
            case BLOCKED:
                return "blocked";
            case BUILT:
            case EXITED_ZERO:
                return "succeeded";
            case ERROR:
            case EXITED_NON_ZERO:
                return "failed";
            default:
                return "";
        }
    }
}