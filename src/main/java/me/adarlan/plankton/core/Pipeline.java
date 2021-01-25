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
import lombok.ToString;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.util.Colors;

@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class Pipeline {

    final ComposeDocument compose;
    final ContainerRuntimeAdapter adapter;

    private final String id;

    private final Set<Job> jobs = new HashSet<>();
    private final Map<String, Job> jobsByName = new HashMap<>();

    final Duration timeoutLimitForJobs;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    Integer biggestJobNameLength;
    private static final String LOADING = "Loading " + Pipeline.class.getSimpleName() + " ... ";

    public Pipeline(PipelineConfiguration configuration) {

        logger.info(LOADING);

        this.adapter = configuration.containerRuntimeAdapter();
        this.compose = configuration.composeDocument();
        this.id = compose.projectName();

        this.timeoutLimitForJobs = Duration.of(1L, ChronoUnit.MINUTES);
        // TODO read from configuration

        logger.info("{}id={}", LOADING, id);
        logger.info("{}compose={}", LOADING, compose);
        logger.info("{}adapter={}", LOADING, adapter);

        instantiateJobs();
        jobs.forEach(this::initializeJobScaleAndInstances);
        jobs.forEach(this::initializeJobDependencyMap);
        jobs.forEach(this::initializeJobDependencyLevel);
        this.initializeInstanceNamesAndBiggestName();
        this.initializeColors();
    }

    private void instantiateJobs() {
        logger.trace("{}Instantiating jobs", LOADING);
        compose.services().forEach(service -> {
            Job job = new Job(this, service);
            this.jobs.add(job);
            this.jobsByName.put(job.name, job);
        });
        logger.info("{}jobs={}", LOADING, jobs);
    }

    private void initializeJobScaleAndInstances(Job job) {
        logger.trace("{}Initializing {}.scale", LOADING, job.name);
        job.scale = job.service.scale();
        logger.info("{}{}.scale={}", LOADING, job.name, job.scale);

        logger.trace("{}Initializing {}.instances", LOADING, job.name);
        for (int instanceIndex = 0; instanceIndex < job.scale; instanceIndex++) {
            JobInstance instance = new JobInstance(job, instanceIndex);
            job.instances.add(instance);
        }
        logger.info("{}{}.instances={}", LOADING, job.name, job.instances);
    }

    private void initializeJobDependencyMap(Job job) {
        ComposeService service = compose.serviceOfName(job.name);
        service.dependsOn().forEach((requiredJobName, condition) -> {
            Job requiredJob = getJobByName(requiredJobName);
            job.dependencyMap.put(requiredJob, condition);
        });
        logger.info("{}{}.dependencyMap={}", LOADING, job.name, job.dependencyMap);
    }

    private void initializeInstanceNamesAndBiggestName() {
        biggestJobNameLength = 0;
        for (Job job : enabledJobs()) {
            for (JobInstance instance : job.instances) {
                if (job.scale() == 1) {
                    instance.name = job.name;
                } else {
                    instance.name = job.name + "_" + instance.index;
                }
                int len = instance.name.length();
                if (len > biggestJobNameLength) {
                    biggestJobNameLength = len;
                }
            }
        }
    }

    private void initializeColors() {
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_YELLOW);
        list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_CYAN);
        list.add(Colors.BRIGHT_PURPLE);
        list.add(Colors.BRIGHT_RED);
        int jobIndex = 0;
        for (Job job : enabledJobs()) {
            int colorIndex = jobIndex % list.size();
            job.color = list.get(colorIndex);
            jobIndex++;
            job.prefix = Utils.prefixOf(job);
            for (JobInstance instance : job.instances) {
                instance.prefix = Utils.prefixOf(instance);
            }
        }
    }

    private void initializeJobDependencyLevel(Job job) {
        logger.trace("{}Initializing {}.dependencyLevel", LOADING, job.name);
        dependencyLevelOf(job, new HashSet<>());
        logger.info("{}{}.dependencyLevel={}", LOADING, job.name, job.dependencyLevel);
    }

    private int dependencyLevelOf(Job job, Set<Job> knownDependents) {
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (Job requiredJob : job.dependencyMap.keySet()) {
                if (knownDependents.contains(requiredJob)) {
                    throw new JobDependencyLoopException();
                }
                int d = dependencyLevelOf(requiredJob, knownDependents);
                if (d > maxDepth)
                    maxDepth = d;
            }
            job.dependencyLevel = maxDepth + 1;
        }
        return job.dependencyLevel;
    }

    public void start() {
        logger.info("Starting {}", this);
        Set<Job> enabledJobs = enabledJobs();
        if (enabledJobs.isEmpty()) {
            logger.info("{} -> {}", this, "There is no jobs to run");
        }
        enabledJobs.forEach(Job::start);
    }

    public void stop() {
        logger.info("Stopping {}", this);
        jobs.forEach(Job::stop);
    }

    void refresh() {
        if (waitingOrRunningJobs().isEmpty()) {
            logger.info("Finished {}", this);
        }
    }

    public String id() {
        return id;
    }

    public Set<Job> jobs() {
        return Collections.unmodifiableSet(jobs);
    }

    public Job getJobByName(String jobName) {
        return jobsByName.get(jobName);
    }

    public Set<Job> enabledJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isEnabled).collect(Collectors.toSet()));
    }

    public Set<Job> waitingOrRunningJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isWaitingOrRunning).collect(Collectors.toSet()));
    }
}
