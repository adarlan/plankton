package me.adarlan.dockerflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import me.adarlan.dockerflow.compose.DockerCompose;
import me.adarlan.dockerflow.rules.RequireFailure;
import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequirePort;
import me.adarlan.dockerflow.rules.RequireSuccess;

@Component
public class Pipeline {

    private final DockerCompose dockerCompose;

    private final Set<Job> jobs = new HashSet<>();

    private final Map<String, Job> jobsByName = new HashMap<>();

    private final Map<Job, Map<String, String>> labelsByJobAndName = new HashMap<>();

    private final Map<Integer, Job> externalPorts = new HashMap<>();

    @Autowired
    public Pipeline(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobRules);
        jobs.forEach(job -> initializeJobDependencies(job, new HashSet<>()));
        jobs.forEach(this::initializeJobStatus);
    }

    private void instantiateJobs() {
        dockerCompose.getServices().forEach((serviceName, serviceObject) -> {
            if (!serviceName.equals("dockerflow")) {
                Job job = new Job();
                job.name = serviceName;
                this.jobs.add(job);
                this.jobsByName.put(job.name, job);
            }
        });
    }

    private void initializeJobLabels(Job job) {
        Map<String, String> labelsByName = dockerCompose.getServicePropertyAsKeyValueMap(job.name, "labels");
        labelsByJobAndName.put(job, labelsByName);
    }

    private void initializeJobExpression(Job job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "dockerflow.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.expression = labelsByName.get(labelName);
        }
    }

    private void initializeJobTimeout(Job job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "dockerflow.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            job.timeout = Long.parseLong(labelValue);
            job.timeoutUnit = TimeUnit.MINUTES; // TODO aceitar formatos: 1s, 1m, 1h, 1d etc
        } else {
            job.timeout = 3L;// TODO Usar uma configuração --dockerflow.timeout.max ou algo assim
            job.timeoutUnit = TimeUnit.MINUTES;
        }
    }

    private void initializeExternalPorts(Job job) {
        List<String> list = dockerCompose.getServicePropertyAsStringList(job.name, "ports");
        list.forEach(string -> {
            String[] spl = string.split("\\:");
            Integer externalPort = Integer.parseInt(spl[0]);
            externalPorts.put(externalPort, job);
        });
    }

    private void initializeJobRules(Job job) {
        job.rules = new HashSet<>();
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        labelsByName.forEach((labelName, labelValue) -> {
            String ruleName = labelName.substring(11);

            if (stringMatchesRegex(labelName, "^dockerflow\\.wait\\.success\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireSuccess rule = new RequireSuccess(job, ruleName, requiredJob);
                job.rules.add(rule);
            }

            if (stringMatchesRegex(labelName, "^dockerflow\\.wait\\.failure\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireFailure rule = new RequireFailure(job, ruleName, requiredJob);
                job.rules.add(rule);
            }

            else if (stringMatchesRegex(labelName, "^dockerflow\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                Job requiredJob = externalPorts.get(port);
                RequirePort rule = new RequirePort(job, ruleName, requiredJob, port);
                job.rules.add(rule);
            }

            else if (stringMatchesRegex(labelName, "^dockerflow\\.wait\\.files$")) {
                String filePath = ((String) labelValue);
                RequireFile rule = new RequireFile(job, ruleName, filePath);
                job.rules.add(rule);
            }
        });
    }

    private static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
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
                if (rule instanceof RuleWithDependency) {
                    Job dependency = ((RuleWithDependency) rule).getRequiredJob();
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

    private void initializeJobStatus(Job job) {
        job.status = JobStatus.WAITING;
        // TODO evaluate expression; if false: job.status = JobStatus.DISABLED;
    }

    public Set<Job> getJobs() {
        return new HashSet<>(jobs);
    }

    public Set<Job> getJobsByStatus(@NonNull JobStatus status) {
        return jobs.stream().filter(j -> j.getStatus().equals(status)).collect(Collectors.toSet());
    }

    public Job getJobByName(@NonNull String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new DockerflowException("Job not found: " + jobName);
        return jobsByName.get(jobName);
    }

    public boolean isDone() {
        boolean done = true;
        for (Job job : jobs) {
            if (job.finalStatus == null) {
                done = false;
            }
        }
        return done;
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