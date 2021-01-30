package me.adarlan.plankton.core;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.util.Colors;
import me.adarlan.plankton.util.LogUtils;

@EqualsAndHashCode(of = "compose")
public class Pipeline {

    final ComposeDocument compose;
    final ContainerRuntimeAdapter adapter;

    private final List<Job> jobs = new ArrayList<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private List<Set<Job>> dependencyLevels = new ArrayList<>();

    final Duration timeoutLimitForJobs;

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    public Pipeline(PipelineConfiguration configuration) {

        this.adapter = configuration.containerRuntimeAdapter();
        this.compose = configuration.composeDocument();

        this.timeoutLimitForJobs = Duration.of(1L, ChronoUnit.HOURS);
        // TODO read from configuration

        initializeLogPrefixLength();
        instantiateJobs();
        jobs.forEach(this::initializeJobScaleAndInstances);
        jobs.forEach(this::initializeJobDependencyMap);
        jobs.forEach(this::initializeJobDependencyLevel);
        sortJobsByDependencyLevel();
    }

    @Override
    public String toString() {
        return Pipeline.class.getSimpleName();
    }

    private void initializeLogPrefixLength() {
        Set<String> ns = new HashSet<>();
        compose.services().forEach(service -> {
            String n;
            if (service.scale() == 1)
                n = service.name();
            else
                n = service.name() + "[" + service.scale() + "]";
            ns.add(n);
        });
        LogUtils.initializePrefixLength(ns);
    }

    private void instantiateJobs() {
        compose.services().forEach(service -> {
            Job job = new Job(this, service);
            this.jobs.add(job);
            this.jobsByName.put(job.name, job);
        });
        logger.debug("Jobs: {}", jobs);
    }

    private void initializeJobScaleAndInstances(Job job) {

        job.scale = job.service.scale();
        if (job.scale > 1)
            logger.debug("{} ... Scale: {}", job.logPrefix, job.scale);

        for (int instanceIndex = 0; instanceIndex < job.scale; instanceIndex++) {
            JobInstance instance = new JobInstance(job, instanceIndex);
            job.instances.add(instance);
        }
    }

    private void initializeJobDependencyMap(Job job) {
        ComposeService service = compose.serviceOfName(job.name);
        service.dependsOn().forEach((requiredJobName, condition) -> {
            Job requiredJob = getJobByName(requiredJobName);
            job.dependencyMap.put(requiredJob, condition);
        });
        if (!job.dependencyMap.isEmpty())
            logger.debug("{} ... Dependencies: {}", job.logPrefix, job.dependencyMap);
    }

    private void initializeJobDependencyLevel(Job job) {
        int dependencyLevel = dependencyLevelOf(job, new HashSet<>());
        for (int i = dependencyLevels.size(); i <= dependencyLevel; i++)
            dependencyLevels.add(new HashSet<>());
        Set<Job> dependencyLevelJobs = dependencyLevels.get(dependencyLevel);
        dependencyLevelJobs.add(job);
        logger.debug("{} ... Dependency level: {}", job.logPrefix, job.dependencyLevel);
    }

    private int dependencyLevelOf(Job job, Set<Job> knownDependents) {
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (Job requiredJob : job.dependencyMap.keySet()) {
                if (knownDependents.contains(requiredJob)) {
                    throw new JobDependencyLoopException();
                }
                requiredJob.directDependents.add(job);
                int d = dependencyLevelOf(requiredJob, knownDependents);
                if (d > maxDepth)
                    maxDepth = d;
            }
            job.dependencyLevel = maxDepth + 1;
        }
        return job.dependencyLevel;
    }

    private void sortJobsByDependencyLevel() {
        Collections.sort(jobs, (job1, job2) -> job1.dependencyLevel().compareTo(job2.dependencyLevel()));
    }

    public void start() {
        logger.info("{}PIPELINE STARTED{}", Colors.BRIGHT_GREEN, Colors.ANSI_RESET);
        startThreadsForCreateContainers();
        if (jobs.isEmpty()) {
            logger.warn("{} ... {}", this, "There are no jobs to run");
        }
        jobs.forEach(Job::start);
    }

    private List<Job> jobsWithoutDependency() {
        return jobs.stream().filter(job -> job.dependencyLevel == 0).collect(Collectors.toList());
    }

    private void startThreadsForCreateContainers() {
        jobsWithoutDependency().forEach(job -> {
            Thread thread = new Thread(() -> createContainersOfJobAndItsDependents(job));
            thread.setUncaughtExceptionHandler((t, e) -> {
                throw new PipelineException("Unable to create containers of " + job + " and its dependents");
            });
            thread.start();
        });
    }

    private void createContainersOfJobAndItsDependents(Job job) {
        if (!job.service.build().isPresent())
            return;
        job.waitCreateContainers();
        job.directDependents.forEach(this::createContainersOfJobAndItsDependents);
    }

    private boolean finished = false;
    private boolean allJobsSucceeded = false;

    synchronized void refresh() {
        finished = true;
        allJobsSucceeded = true;
        for (Job job : jobs) {
            if (job.isWaitingOrRunning()) {
                finished = false;
                allJobsSucceeded = false;
            } else if (!job.isSucceeded()) {
                allJobsSucceeded = false;
            }
        }
        if (finished) {
            if (allJobsSucceeded)
                logger.info("{}PIPELINE FINISHED WITH SUCCESS{}", Colors.BRIGHT_GREEN, Colors.ANSI_RESET);
            else
                logger.error("{}PIPELINE FINISHED WITH FAILURE{}", Colors.BRIGHT_RED, Colors.ANSI_RESET);
            jobs.forEach(Job::info);
        }
    }

    public void stop() {
        logger.debug("{} ... Stopping", this);
        jobs.forEach(Job::stop);
    }

    public List<Job> jobs() {
        return Collections.unmodifiableList(jobs);
    }

    public Job getJobByName(String jobName) {
        return jobsByName.get(jobName);
    }
}
