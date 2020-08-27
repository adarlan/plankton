package me.adarlan.dockerflow;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequireServicePort;
import me.adarlan.dockerflow.rules.RequireTaskStatus;

@Component
public class Pipeline {

    private final ApplicationConfig applicationConfig;

    private final Map<String, Object> dockerCompose;

    private final Map<String, Object> dockerComposeServices;

    private final Set<Job> jobs = new HashSet<>();

    private final Map<String, Job> jobByName = new HashMap<>();

    @Autowired
    public Pipeline(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;

        this.dockerCompose = Utils.createMapFromYamlFile(this.applicationConfig.getFile());
        this.dockerComposeServices = Utils.getPropertyMap(dockerCompose, "services");

        dockerComposeServices.forEach((serviceName, serviceObject) -> {
            if (!serviceName.equals("dockerflow")) {

                Job job = new Job();
                job.name = serviceName;
                setStatus(job, JobStatus.WAITING);

                this.jobs.add(job);
                this.jobByName.put(job.getName(), job);
            }
        });

        jobs.forEach(this::initializeRules);
        jobs.forEach(job -> initializeDependencies(job, new HashSet<>()));
    }

    public Set<Job> getJobs() {
        return new HashSet<>(jobs);
    }

    public Job getJobByName(String jobName) {
        return jobByName.get(jobName);
    }

    void setStatus(Job job, JobStatus status) {
        job.status = status;
        switch (status) {
            case WAITING:
                break;
            case RUNNING:
                job.initialInstant = Instant.now();
                break;
            case CANCELED:
            case INTERRUPTED:
            case FAILED:
            case TIMEOUT:
            case FINISHED:
                job.finalInstant = Instant.now();
                job.finalStatus = status;
                break;
            default:
                break;
        }
        System.out.println(applicationConfig.getName() + "::" + job.name + " -> " + job.status);
    }

    private void initializeRules(Job job) {

        job.rules = new HashSet<>();

        Map<String, Object> service = Utils.getPropertyMap(dockerComposeServices, job.getName());
        Map<String, Object> labels = Utils.getPropertyMap(service, "labels");

        labels.forEach((labelName, labelValue) -> {
            String[] splitedLabelName = labelName.split("\\.");

            if (matches(labelName, "^dockerflow\\.wait\\.[\\w-]+\\.status$")) {
                String ruleName = labelName.substring(11);
                String requiredJobName = splitedLabelName[2];
                Job requiredJob = this.getJobByName(requiredJobName);
                JobStatus requiredStatus = JobStatus.valueOf(labelValue.toString().toUpperCase());
                RequireTaskStatus rule = new RequireTaskStatus(job, ruleName, requiredJob, requiredStatus);
                job.rules.add(rule);
            }

            if (matches(labelName, "^dockerflow\\.wait\\.[\\w-]+\\.port$")) {
                String ruleName = labelName.substring(11);
                String requiredJobName = splitedLabelName[2];
                Job requiredJob = this.getJobByName(requiredJobName);
                Integer port = Integer.parseInt(labelValue.toString());
                RequireServicePort rule = new RequireServicePort(job, ruleName, requiredJob, port);
                job.rules.add(rule);
            }

            if (matches(labelName, "^dockerflow\\.wait\\.file$")) {
                String ruleName = labelName.substring(11);
                String filePath = labelValue.toString();
                RequireFile rule = new RequireFile(job, ruleName, filePath);
                job.rules.add(rule);
            }
        });
    }

    private boolean matches(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
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
                if (rule instanceof RuleDependency) {
                    Job dependency = ((RuleDependency) rule).getRequiredJob();
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