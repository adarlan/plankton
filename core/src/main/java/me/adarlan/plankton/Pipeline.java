package me.adarlan.plankton;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import me.adarlan.plankton.rules.RequireFailure;
import me.adarlan.plankton.rules.RequireFile;
import me.adarlan.plankton.rules.RequirePort;
import me.adarlan.plankton.rules.RequireSuccess;
import me.adarlan.plankton.util.RegexUtil;

public class Pipeline {

    @Getter
    private final DockerCompose dockerCompose;

    @Getter
    private final String name;

    // private final List<String> variables;

    private final Set<Job> jobs = new HashSet<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private final Map<Job, Map<String, String>> labelsByJobAndName = new HashMap<>();
    private final Map<Integer, Job> externalPorts = new HashMap<>();

    private final Logger logger = Logger.getLogger();

    public Pipeline(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
        this.name = dockerCompose.getProjectName();
        // this.variables = dockerCompose.variables;
        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeNeedToBuild);
        jobs.forEach(this::initializeJobScale);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobRules);
        // jobs.forEach(this::initializeJobDependencies);
        // jobs.forEach(this::initializeJobVariables);
        jobs.forEach(Job::initializeStatus);
    }

    private void instantiateJobs() {
        dockerCompose.getServiceNames().forEach(serviceName -> {
            Job job = new Job(this, serviceName);
            this.jobs.add(job);
            this.jobsByName.put(serviceName, job);
        });
    }

    private void initializeJobLabels(Job job) {
        Map<String, String> labelsByName = dockerCompose.getServiceLabels(job.getName());
        labelsByJobAndName.put(job, labelsByName);
    }

    private void initializeJobExpression(Job job) {
        // TODO validate synthax
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.setExpression(labelsByName.get(labelName));
        }
    }

    private void initializeNeedToBuild(Job job) {
        Map<String, Object> service = dockerCompose.getService(job.getName());
        if (service.containsKey("build")) {
            job.setNeedToBuild(true);
        } else {
            job.setNeedToBuild(false);
        }
    }

    private void initializeJobScale(Job job) {
        // TODO usar label: plankton.scale
        // a propriedade deploy.replicas só funciona pra Swarm
        job.setScale(1);
    }

    private void initializeJobTimeout(Job job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            // TODO aceitar formatos: 1s, 1m, 1h, 1d etc
            String labelValue = labelsByName.get(labelName);
            job.initializeTimeout(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            // TODO Usar uma configuração --plankton.timeout.max ou algo assim
            job.initializeTimeout(1L, ChronoUnit.MINUTES);
        }
    }

    private void initializeExternalPorts(Job job) {
        List<Map<String, Object>> ports = dockerCompose.getServicePorts(job.getName());
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO published pode ser null
            externalPorts.put(externalPort, job);
        });
    }

    private void initializeJobRules(Job job) {
        Set<Rule> rules = new HashSet<>();
        job.setRules(rules);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        labelsByName.forEach((labelName, labelValue) -> {
            String ruleName = labelName.substring(11);

            if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireSuccess rule = new RequireSuccess(job, ruleName, requiredJob);
                rules.add(rule);
            }

            if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                RequireFailure rule = new RequireFailure(job, ruleName, requiredJob);
                rules.add(rule);
            }

            else if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                // TODO isso tá estranho pq a porta informada é a 'published',
                // mas a porta usada internamente pelo container é a 'target'
                Integer port = Integer.parseInt(labelValue);
                Job requiredJob = externalPorts.get(port);
                RequirePort rule = new RequirePort(job, ruleName, requiredJob, port);
                rules.add(rule);
            }

            else if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.files$")) {
                // TODO remover esta rule?
                // o ruim dela é que não tem como saber se bloqueou, a não ser que todos os
                // demais jobs tenham terminado
                String filePath = labelValue;
                RequireFile rule = new RequireFile(job, ruleName, filePath);
                rules.add(rule);
            }
        });
    }

    /*
     * private void initializeJobDependencies(Job job) {
     * initializeJobDependencies(job, new HashSet<>()); }
     * 
     * private int initializeJobDependencies(Job job, Set<Job> knownDependents) { if
     * (job.allDependents == null) { job.allDependents = new HashSet<>(); }
     * knownDependents.forEach(kd -> job.allDependents.add(kd)); job.allDependencies
     * = new HashSet<>(); job.directDependencies = new HashSet<>();
     * job.dependencyLevel = null; if (job.dependencyLevel == null) {
     * knownDependents.add(job); int maxDepth = -1; for (Rule rule : job.rules) { if
     * (rule instanceof RuleWithDependency) { Job dependency = ((RuleWithDependency)
     * rule).getRequiredJob(); if (knownDependents.contains(dependency)) { throw new
     * PlanktonException("Dependency loop"); } int d =
     * initializeJobDependencies(dependency, knownDependents); if (d > maxDepth)
     * maxDepth = d; job.allDependencies.add(dependency);
     * job.allDependencies.addAll(dependency.allDependencies);
     * job.directDependencies.add(dependency); } } job.dependencyLevel = maxDepth +
     * 1; } return job.dependencyLevel; }
     */

    /*
     * private void initializeJobVariables(Job job) { job.setVariables(new
     * ArrayList<>()); // TODO add all variables in .env (it is Docker (or Docker
     * Compose?) default) // TODO add all variables in SERVICE.env_file // TODO add
     * all variables in SERVICE.environment }
     */

    public void run() throws InterruptedException {
        boolean done = false;
        while (!done) {
            done = true;
            for (Job job : jobs) {
                job.refresh();
                if (!job.hasEnded()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info(() -> "That's All Folks!");
        // TODO mostrar resumo... quantos SUCCESS, quantos FAILURE, etc
        // TODO salvar o estado em um arquivo nos metradados
    }

    // public List<String> getVariables() {
    // return Collections.unmodifiableList(variables);
    // }

    public Set<Job> getJobs() {
        return Collections.unmodifiableSet(jobs);
    }

    public Job getJobByName(@NonNull String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new PlanktonException("Job not found: " + jobName);
        return jobsByName.get(jobName);
    }
}