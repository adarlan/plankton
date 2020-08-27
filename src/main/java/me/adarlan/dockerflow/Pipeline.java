package me.adarlan.dockerflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequirePort;
import me.adarlan.dockerflow.rules.RequireStatus;

@Component
public class Pipeline {

    private final DockerflowConfig dockerflowConfig;

    private final Map<String, Object> dockerCompose;

    private final Map<String, Object> dockerComposeServices;

    private final Set<Job> jobs = new HashSet<>();

    private final Map<String, Job> jobsByName = new HashMap<>();

    private final Map<Job, Map<String, Object>> labelsByJobAndName = new HashMap<>();

    @Autowired
    public Pipeline(DockerflowConfig dockerflowConfig) {
        this.dockerflowConfig = dockerflowConfig;
        this.dockerCompose = Utils.createMapFromYamlFile(this.dockerflowConfig.getFile());
        this.dockerComposeServices = Utils.getPropertyMap(dockerCompose, "services");
        dockerComposeServices.forEach((serviceName, serviceObject) -> {
            if (!serviceName.equals("dockerflow")) {
                Job job = new Job();
                job.name = serviceName;
                this.jobs.add(job);
                this.jobsByName.put(job.name, job);
            }
        });
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeJobStatus);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeJobRules);
        jobs.forEach(job -> initializeJobDependencies(job, new HashSet<>()));
    }

    private void initializeJobLabels(Job job) {
        Map<String, Object> dcService = (Map<String, Object>) dockerComposeServices.get(job.name);
        Object dcLabels = dcService.get("labels");
        Map<String, Object> labelsByName;
        if (dcLabels == null) {
            labelsByName = new HashMap<>();
        } else if (dcLabels instanceof Map) {
            labelsByName = (Map<String, Object>) dcLabels;
        } else {
            List<String> dcLabelsList = (List<String>) dcLabels;
            labelsByName = new HashMap<>();
            dcLabelsList.forEach(labelString -> {
                int separatorIndex = labelString.indexOf("=");
                String labelKey = labelString.substring(0, separatorIndex).trim();
                String labelValue = labelString.substring(separatorIndex + 1).trim();
                labelsByName.put(labelKey, labelValue);
            });
        }
        Map<String, Object> onlyDockerflowLabels = new HashMap<>();
        labelsByName.forEach((k, v) -> {
            if (k.startsWith("dockerflow."))
                onlyDockerflowLabels.put(k, v);
        });
        labelsByJobAndName.put(job, onlyDockerflowLabels);
    }

    private void initializeJobExpression(Job job) {
        Object labelValue = labelsByJobAndName.get(job).get("dockerflow.enable.if");
        if (labelValue != null) {
            job.expression = (String) labelValue;
        }
    }

    private void initializeJobStatus(Job job) {
        job.status = JobStatus.WAITING;
        // TODO calcular expression
    }

    private void initializeJobTimeout(Job job) {
        Object labelValue = labelsByJobAndName.get(job).get("dockerflow.timeout");
        if (labelValue != null) {
            job.timeout = Long.parseLong(labelValue.toString());
            job.timeoutUnit = TimeUnit.MINUTES;
        } else {
            job.timeout = 3L;// TODO Usar uma configuração --dockerflow.timeout.max ou algo assim
            job.timeoutUnit = TimeUnit.MINUTES;
        }
    }

    private void initializeJobRules(Job job) {
        job.rules = new HashSet<>();
        labelsByJobAndName.get(job).forEach((labelName, labelValue) -> {
            String ruleName = labelName.substring(11);
            String[] splitedLabelName = labelName.split("\\.");

            if (Utils.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.[\\w-]+\\.status$")) {
                String requiredJobName = splitedLabelName[2];
                Job requiredJob = this.getJobByName(requiredJobName);
                JobStatus requiredStatus = JobStatus.valueOf(((String) labelValue).toUpperCase());
                RequireStatus rule = new RequireStatus(job, ruleName, requiredJob, requiredStatus);
                job.rules.add(rule);
            }

            else if (Utils.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.[\\w-]+\\.port$")) {
                String requiredJobName = splitedLabelName[2];
                Job requiredJob = this.getJobByName(requiredJobName);
                Integer port = Integer.parseInt(labelValue.toString());
                RequirePort rule = new RequirePort(job, ruleName, requiredJob, port);
                job.rules.add(rule);
            }

            else if (Utils.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.file$")) {
                String filePath = ((String) labelValue);
                RequireFile rule = new RequireFile(job, ruleName, filePath);
                job.rules.add(rule);
            }
        });
    }

    private int initializeJobDependencies(Job job, Set<Job> knownDependents) {
        knownDependents.forEach(kd -> job.allDependents.add(kd));
        job.allDependencies = new HashSet<>();
        job.directDependencies = new HashSet<>();
        job.dependencyLevel = null;
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (Rule rule : job.rules) {
                if (rule instanceof RuleDependency) {
                    Job dependency = ((RuleDependency) rule).getRequiredJob();
                    if (knownDependents.contains(dependency)) {
                        throw new DockerflowException("Dependency loop");
                    }
                    int d = initializeJobDependencies(dependency, knownDependents);
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

    public Set<Job> getJobs() {
        return new HashSet<>(jobs);
    }

    public Set<Job> getJobsByStatus(@NonNull JobStatus status) {
        Set<Job> set = new HashSet<>(jobs);
        jobs.forEach(job -> {
            if (job.status.equals(status))
                set.add(job);
        });
        return set;
    }

    public Job getJobByName(@NonNull String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new DockerflowException("Job not found: " + jobName);
        return jobsByName.get(jobName);
    }

    @lombok.Data
    public static class Data {
        Set<Job.Data> jobs = new HashSet<>();
    }

    public Data getData() {
        Data data = new Data();
        this.jobs.forEach(job -> data.jobs.add(job.getData()));
        return data;
    }
}