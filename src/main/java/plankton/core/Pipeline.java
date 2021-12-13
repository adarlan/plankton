package plankton.core;

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
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;
import plankton.util.Colors;
import plankton.util.LogUtils;

@EqualsAndHashCode(of = "composeDocument")
public class Pipeline {

    final ComposeDocument composeDocument;
    final ContainerRuntimeAdapter containerRuntimeAdapter;

    private final List<Job> jobs = new ArrayList<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private List<Set<Job>> dependencyLevels = new ArrayList<>();

    final Duration timeoutLimitForJobs;

    final Set<Job> autoStopJobs = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    public Pipeline(PipelineConfiguration configuration) {

        this.containerRuntimeAdapter = configuration.containerRuntimeAdapter();
        this.composeDocument = configuration.composeDocument();

        this.timeoutLimitForJobs = Duration.of(1L, ChronoUnit.HOURS);
        // TODO read from configuration

        initializeLogPrefixLength();
        instantiateJobs();
        jobs.forEach(this::initializeJobInstances);
        jobs.forEach(this::initializeJobDependencyMap);
        jobs.forEach(this::initializeJobDependencyLevel);
        sortJobsByDependencyLevel();
        jobs.forEach(this::initializeJobStopMode);
        jobs.forEach(job -> job.composeService.logInfo());

        logger.debug("Pipeline.autoStopJobs: {}", autoStopJobs);
    }

    @Override
    public String toString() {
        return Pipeline.class.getSimpleName();
    }

    private Set<ComposeService> composeServices() {
        return composeDocument.services().stream().filter(s -> !s.name().startsWith(".")).collect(Collectors.toSet());
    }

    private void initializeLogPrefixLength() {
        Set<String> ns = new HashSet<>();
        composeServices().forEach(composeService -> {
            String n;
            if (composeService.scale() > 1)
                n = composeService.name() + "[" + composeService.scale() + "]";
            else
                n = composeService.name();
            ns.add(n);
        });
        LogUtils.initializePrefixLength(ns);
    }

    private void instantiateJobs() {
        composeServices().forEach(composeService -> {
            Job job = new Job(this, composeService);
            this.jobs.add(job);
            this.jobsByName.put(job.name, job);
        });
        logger.debug("Jobs: {}", jobs);
    }

    private void initializeJobInstances(Job job) {
        for (int instanceIndex = 0; instanceIndex < job.composeService.scale(); instanceIndex++) {
            JobInstance instance = new JobInstance(job, instanceIndex);
            job.instances.add(instance);
        }
    }

    private void initializeJobDependencyMap(Job job) {
        ComposeService composeService = composeDocument.serviceOfName(job.name);
        composeService.dependsOn().forEach((requiredJobName, condition) -> {
            Job requiredJob = getJobByName(requiredJobName);
            job.dependencyMap.put(requiredJob, condition);
            requiredJob.requiredConditions.add(condition);
        });
        if (!job.dependencyMap.isEmpty())
            logger.debug("{} ... Dependencies: {}", job.logPrefix, job.dependencyMap);
    }

    private void initializeJobDependencyLevel(Job job) {
        int dependencyLevel = dependencyLevelOf(job, new ArrayList<>());
        for (int i = dependencyLevels.size(); i <= dependencyLevel; i++)
            dependencyLevels.add(new HashSet<>());
        Set<Job> dependencyLevelJobs = dependencyLevels.get(dependencyLevel);
        dependencyLevelJobs.add(job);
        logger.debug("{} ... Dependency level: {}", job.logPrefix, job.dependencyLevel);
    }

    private int dependencyLevelOf(Job job, List<Job> knownDependents) {
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (Job requiredJob : job.dependencyMap.keySet()) {
                if (knownDependents.contains(requiredJob)) {
                    throw new JobDependencyLoopException(
                            requiredJob + " is one of its dependents: " + knownDependents.toString());
                }
                requiredJob.directDependents.add(job);
                int d = dependencyLevelOf(requiredJob, new ArrayList<>(knownDependents));
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

    private void initializeJobStopMode(Job job) {
        if ((job.requiredConditions.contains(DependsOnCondition.SERVICE_STARTED)
                || job.requiredConditions.contains(DependsOnCondition.SERVICE_HEALTHY))
                && !(job.requiredConditions.contains(DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY)
                        || job.requiredConditions.contains(DependsOnCondition.SERVICE_FAILED))) {
            job.autoStopWhenDirectDependentsHaveFinalStatus = true;
            autoStopJobs.add(job);
        }
    }

    public void start() {
        line();
        logger.info("{}                            PIPELINE STARTED{}", Colors.BRIGHT_WHITE, Colors.ANSI_RESET);
        line();
        if (jobs.isEmpty()) {
            logger.warn("{} ... {}", this, "There are no jobs to run");
        }
        jobs.forEach(Job::start);
    }

    private boolean finished = false;

    synchronized void refresh() {
        if (finished)
            return;
        autoStopJobs();
        refreshFinished();
        if (finished) {
            line();
            logger.info("{}                           PIPELINE FINISHED{}", Colors.BRIGHT_WHITE, Colors.ANSI_RESET);
            line();
            jobs.forEach(Job::logFinalStatus);
            line();
        }
    }

    private void refreshFinished() {
        boolean f = true;
        for (Job job : jobs) {
            if (!job.status.isFinal())
                f = false;
        }
        finished = f;
    }

    private void autoStopJobs() {
        for (Job autoStopJob : autoStopJobs) {
            boolean autoStopNow = true;
            for (Job dependentJob : autoStopJob.directDependents) {
                if (!dependentJob.status.isFinal()) {
                    autoStopNow = false;
                }
            }
            if (autoStopNow) {
                logger.debug("Auto stopping {} because it is not required anymore", autoStopJob);
                autoStopJob.stop();
            }
        }
    }

    private void line() {
        String line = "------------------------------------------------------------------------";
        logger.info("{}{}{}", Colors.BRIGHT_WHITE, line, Colors.ANSI_RESET);
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
