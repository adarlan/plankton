package me.adarlan.dockerflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import me.adarlan.dockerflow.bash.BashScript;
import me.adarlan.dockerflow.rules.RequireFailure;
import me.adarlan.dockerflow.rules.RequireFile;
import me.adarlan.dockerflow.rules.RequirePort;
import me.adarlan.dockerflow.rules.RequireSuccess;
import me.adarlan.dockerflow.util.RegexUtil;

@Component
public class Pipeline {

    private final DockerCompose dockerCompose;
    private final List<String> environmentVariables;

    final Set<Job> jobs = new HashSet<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private final Map<Job, Map<String, String>> labelsByJobAndName = new HashMap<>();
    private final Map<Integer, Job> externalPorts = new HashMap<>();

    @Autowired
    public Pipeline(DockerCompose dockerCompose, List<String> environmentVariables) {
        this.dockerCompose = dockerCompose;
        this.environmentVariables = environmentVariables;
        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeJobScaleAndLogs);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobRules);
        jobs.forEach(this::initializeJobDependencies);
        jobs.forEach(this::initializeJobVariables);
        jobs.forEach(this::evaluateJobExpression);
        jobs.forEach(this::initializeJobStatus);
        Logger.initializeFollow(this, dockerCompose);
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
        // TODO validate synthax
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "dockerflow.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.expression = labelsByName.get(labelName);
        }
    }

    private void initializeJobScaleAndLogs(Job job) {
        // TODO usar label ou usar a propriedade do arquivo?
        job.scale = 1;
        // job.outputLogs = new ArrayList<>();
        // job.errorLogs = new ArrayList<>();
        // for (int i = 0; i < job.scale; i++) {
        // job.outputLogs.add(new ArrayList<>());
        // job.errorLogs.add(new ArrayList<>());
        // }
    }

    private void initializeJobTimeout(Job job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "dockerflow.timeout";
        if (labelsByName.containsKey(labelName)) {
            // TODO aceitar formatos: 1s, 1m, 1h, 1d etc
            String labelValue = labelsByName.get(labelName);
            job.timeout = Long.parseLong(labelValue);
            job.timeoutUnit = TimeUnit.MINUTES;
        } else {
            // TODO Usar uma configuração --dockerflow.timeout.max ou algo assim
            job.timeout = 1L;
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

            if (RegexUtil.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.success\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireSuccess rule = new RequireSuccess(job, ruleName, requiredJob);
                job.rules.add(rule);
            }

            if (RegexUtil.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.failure\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireFailure rule = new RequireFailure(job, ruleName, requiredJob);
                job.rules.add(rule);
            }

            else if (RegexUtil.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.ports$")) {
                // TODO isso tá estranho pq a porta informada é a published,
                // mas a porta usada internamente pelo container é a exposed
                Integer port = Integer.parseInt(labelValue);
                Job requiredJob = externalPorts.get(port);
                RequirePort rule = new RequirePort(job, ruleName, requiredJob, port);
                job.rules.add(rule);
            }

            else if (RegexUtil.stringMatchesRegex(labelName, "^dockerflow\\.wait\\.files$")) {
                // TODO remover esta rule?
                // o ruim dela é que não tem como saber se bloqueou, a não ser que todos os
                // demais jobs tenham terminado
                String filePath = labelValue;
                RequireFile rule = new RequireFile(job, ruleName, filePath);
                job.rules.add(rule);
            }
        });
    }

    private void initializeJobDependencies(Job job) {
        job.allDependents = new HashSet<>();
        initializeJobDependencies(job, new HashSet<>());
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

    private void initializeJobVariables(Job job) {
        job.variables = new ArrayList<>();
        // TODO add all variables in .env (it is Docker (or Docker Compose?) default)
        // TODO add all variables in SERVICE.env_file
        // TODO add all variables in SERVICE.environment
    }

    private void evaluateJobExpression(Job job) {
        if (job.expression == null) {
            return;
        }
        final String scriptName = "evaluate-job-expression_" + job.name;
        BashScript script = new BashScript(scriptName);
        script.command(job.expression);
        script.env(environmentVariables);
        script.env(job.variables);
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        try {
            script.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (script.getExitCode() == 0) {
            job.expressionResult = true;
        } else {
            job.expressionResult = false;
        }
    }

    private void initializeJobStatus(Job job) {
        if (job.expression == null || job.expressionResult.equals(true)) {
            job.state.set(JobStatus.WAITING);
        } else {
            job.state.set(JobStatus.DISABLED);
        }
    }

    public Set<Job> getJobs() {
        return new HashSet<>(jobs);
    }

    public Set<Job> getJobsByStatus(@NonNull JobStatus status) {
        return jobs.stream().filter(j -> j.state.getStatus().equals(status)).collect(Collectors.toSet());
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