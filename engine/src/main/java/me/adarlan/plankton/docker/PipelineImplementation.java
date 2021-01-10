package me.adarlan.plankton.docker;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import me.adarlan.plankton.core.Job;
import me.adarlan.plankton.core.JobDependency;
import me.adarlan.plankton.core.Logger;
import me.adarlan.plankton.core.Pipeline;
import me.adarlan.plankton.core.dependency.WaitDependencyFailure;
import me.adarlan.plankton.core.dependency.WaitDependencyPort;
import me.adarlan.plankton.core.dependency.WaitDependencySuccess;
import me.adarlan.plankton.util.RegexUtil;

class PipelineImplementation implements Pipeline {

    final DockerCompose dockerCompose;

    @Getter
    private final String id;

    private final Set<JobImplementation> jobs = new HashSet<>();
    private final Map<String, JobImplementation> jobsByName = new HashMap<>();
    private final Map<JobImplementation, Map<String, String>> labelsByJobAndName = new HashMap<>();
    private final Map<Integer, JobImplementation> externalPorts = new HashMap<>();

    private final Logger logger = Logger.getLogger();

    PipelineImplementation(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
        this.id = dockerCompose.getProjectName();
        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeNeedToBuild);
        jobs.forEach(this::initializeJobScale);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobDependencies);
        jobs.forEach(JobImplementation::initializeStatus);
    }

    private void instantiateJobs() {
        dockerCompose.getServiceNames().forEach(serviceName -> {
            JobImplementation job = new JobImplementation(this, serviceName);
            this.jobs.add(job);
            this.jobsByName.put(serviceName, job);
        });
    }

    private void initializeJobLabels(JobImplementation job) {
        Map<String, String> labelsByName = dockerCompose.getServiceLabels(job.getName());
        labelsByJobAndName.put(job, labelsByName);
    }

    private void initializeJobExpression(JobImplementation job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.setExpression(labelsByName.get(labelName));
        }
    }

    private void initializeNeedToBuild(JobImplementation job) {
        Map<String, Object> service = dockerCompose.getService(job.getName());
        if (service.containsKey("build")) {
            job.setNeedToBuild(true);
        } else {
            job.setNeedToBuild(false);
        }
    }

    private void initializeJobScale(JobImplementation job) {
        job.setScale(1);
    }

    private void initializeJobTimeout(JobImplementation job) {
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            job.initializeTimeout(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            job.initializeTimeout(1L, ChronoUnit.MINUTES);
        }
    }

    private void initializeExternalPorts(JobImplementation job) {
        List<Map<String, Object>> ports = dockerCompose.getServicePorts(job.getName());
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, job);
        });
    }

    private void initializeJobDependencies(JobImplementation job) {
        Set<JobDependency> dependencies = new HashSet<>();
        job.setDependencies(dependencies);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        labelsByName.forEach((labelName, labelValue) -> {

            if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredJobName = labelValue;
                JobImplementation requiredJob = this.getJobByName(requiredJobName);
                WaitDependencySuccess dependency = new WaitDependencySuccess(job, requiredJob);
                dependencies.add(dependency);
            }

            if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredJobName = labelValue;
                JobImplementation requiredJob = this.getJobByName(requiredJobName);
                WaitDependencyFailure dependency = new WaitDependencyFailure(job, requiredJob);
                dependencies.add(dependency);
            }

            else if (RegexUtil.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                JobImplementation requiredJob = externalPorts.get(port);
                WaitDependencyPort dependency = new WaitDependencyPort(job, requiredJob, port);
                dependencies.add(dependency);
            }
        });
    }

    @Override
    public void run() throws InterruptedException {
        boolean done = false;
        while (!done) {
            done = true;
            for (JobImplementation job : jobs) {
                job.refresh();
                if (!job.hasEnded()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info(() -> "Pipeline finished");
    }

    @Override
    public Set<Job> getJobs() {
        return Collections.unmodifiableSet(jobs);
    }

    @Override
    public JobImplementation getJobByName(@NonNull String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new PlanktonDockerException("Job not found: " + jobName);
        return jobsByName.get(jobName);
    }
}