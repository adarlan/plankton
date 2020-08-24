package me.adarlan.dockerflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class Pipeline {

    @Autowired
    private String pipelineId;

    @Getter
    private final Set<Job> jobs;

    private final Map<String, Job> jobsMap;

    public Pipeline() {
        jobs = new HashSet<>();
        jobsMap = new HashMap<>();
        Map<String, Object> map = Utils.createMapFromYamlFile("docker-compose.yml");
        Map<String, Object> services = Utils.getPropertyMap(map, "services");
        services.forEach((serviceName, serviceData) -> {
            if (!serviceName.equals("dockerflow")) {
                Job job = new Job(this, serviceName, (Map<String, Object>) serviceData);
                jobs.add(job);
                jobsMap.put(job.getName(), job);
            }
        });
        jobs.forEach(Job::initialize);
    }

    public String getId() {
        return pipelineId;
    }

    public Job getJobByName(String name) {
        return jobsMap.get(name);
    }
}