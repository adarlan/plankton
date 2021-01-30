package me.adarlan.plankton.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import me.adarlan.plankton.core.Job;

@Data
public class JobDto {

    String name;
    String status;
    Map<String, String> dependencies = new HashMap<>();

    public JobDto(Job job) {
        this.name = job.name();
        this.status = job.status().toString().toLowerCase();
        job.dependencyMap().forEach((j, c) -> this.dependencies.put(j.name(), c.toString().toLowerCase()));
    }
}
