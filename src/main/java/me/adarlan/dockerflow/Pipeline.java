package me.adarlan.dockerflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequireServicePort;
import me.adarlan.dockerflow.rules.RequireTaskStatus;
import me.adarlan.dockerflow.rules.Rule;

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
        jobs.forEach(this::initializeRules);
        jobs.forEach(job -> initializeDependencies(job, new HashSet<>()));
    }

    public String getId() {
        return pipelineId;
    }

    public Job getJobByName(String name) {
        return jobsMap.get(name);
    }

    private void initializeRules(Job job) {
        job.rules = new HashSet<>();
        Map<String, Object> labels = Utils.getPropertyMap(job.data, "labels");
        labels.forEach((ruleName, value) -> {
            final String[] ss = ruleName.split("\\.");
            if (ss[0].equals("dockerflow")) {
                // TODO usar regex
                if (ss[1].equals("require")) {
                    if (ss[2].equals("service")) {
                        final Job targetJob = this.getJobByName(ss[3]);
                        final String port = (String) labels.get(ruleName);
                        final Rule rule = new RequireServicePort(job, ruleName, targetJob, port);
                        job.rules.add(rule);
                    } else if (ss[2].equals("task")) {
                        final Job targetJob = this.getJobByName(ss[3]);
                        final String statusString = (String) labels.get(ruleName);
                        final JobStatus jobStatus = JobStatus.valueOf(statusString.toUpperCase());
                        final Rule rule = new RequireTaskStatus(job, ruleName, targetJob, jobStatus);
                        job.rules.add(rule);
                    } else if (ss[2].equals("file")) {
                        final String filePath = (String) labels.get(ruleName);
                        final Rule rule = new RequireFile(job, ruleName, filePath);
                        job.rules.add(rule);
                    }
                }
            }
        });
    }

    private int initializeDependencies(Job job, Set<Job> knownDependents) {
        knownDependents.forEach(kd -> job.allDependents.add(kd));
        job.allDependencies = new HashSet<>();
        job.directDependencies = new HashSet<>();
        job.dependencyLevel = null;
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (Rule rule : job.rules) {
                Job dependency = rule.getRequiredJob();
                if (dependency != null) {
                    if (knownDependents.contains(dependency)) {
                        throw new DockerflowException("Dependency loop");
                    }
                    int d = initializeDependencies(dependency, knownDependents);
                    if (d > maxDepth)
                        maxDepth = d;
                    job.allDependencies.add(dependency);
                    job.allDependencies.addAll(dependency.allDependencies);
                    job.directDependencies.add(dependency);
                }
            }
            job.dependencyLevel = maxDepth + 1;
        }
        return job.dependencyLevel;
    }
}