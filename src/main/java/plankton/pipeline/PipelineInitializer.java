package plankton.pipeline;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;
import plankton.util.Colors;
import plankton.util.LogUtils;

import lombok.Getter;

public class PipelineInitializer {

    @Getter
    private final Pipeline pipeline;

    public PipelineInitializer(PipelineConfiguration pipelineConfiguration) {
        pipeline = new Pipeline();
        pipeline.composeDocument = pipelineConfiguration.composeDocument();
        pipeline.containerRuntimeAdapter = pipelineConfiguration.containerRuntimeAdapter();
        pipeline.timeoutLimitForJobs = timeoutLimitForJobs();
        pipeline.composeServices = composeServices();
        initializeLogPrefixLength();
        instantiateJobs();
        pipeline.jobs.forEach(this::initializeJobInstances);
        pipeline.jobs.forEach(this::initializeJobDependencyMap);
        pipeline.jobs.forEach(this::initializeJobDependencyLevel);
        sortJobsByDependencyLevel();
        pipeline.jobs.forEach(this::initializeJobStopMode);
        pipeline.jobs.forEach(job -> job.composeService.logInfo());
    }

    private Set<ComposeService> composeServices() {
        return pipeline.composeDocument.services().stream()
                .filter(s -> !s.name().startsWith("."))
                .collect(Collectors.toSet());
    }

    private Duration timeoutLimitForJobs() {
        return Duration.of(1L, ChronoUnit.HOURS);
        // TODO read from configuration
    }

    private void initializeLogPrefixLength() {
        Set<String> ns = new HashSet<>();
        pipeline.composeServices.forEach(composeService -> {
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
        pipeline.composeServices.forEach(composeService -> {
            Job job = new Job();
            job.pipeline = pipeline;
            job.composeService = composeService;
            job.name = composeService.name();
            job.colorizedName = Colors.colorized(job.name);
            job.logPrefix = LogUtils.prefixOf(job.name);
            pipeline.jobs.add(job);
            pipeline.jobsByName.put(job.name, job);
        });
    }

    private void initializeJobInstances(Job job) {
        for (int instanceIndex = 0; instanceIndex < job.composeService.scale(); instanceIndex++) {
            JobInstance instance = new JobInstance();
            instance.job = job;
            instance.pipeline = job.pipeline;
            instance.index = instanceIndex;
            if (job.composeService.scale() > 1) {
                instance.colorizedName = Colors.colorized(job.name + "_" + instanceIndex, job.name);
                instance.logPrefix = LogUtils.prefixOf(job.name, "[" + instanceIndex + "]");
            } else {
                instance.colorizedName = Colors.colorized(job.name);
                instance.logPrefix = LogUtils.prefixOf(job.name);
            }
            job.instances.add(instance);
        }
    }

    private void initializeJobDependencyMap(Job job) {
        ComposeService composeService = pipeline.composeDocument.serviceOfName(job.name);
        composeService.dependsOn().forEach((requiredJobName, condition) -> {
            Job requiredJob = pipeline.jobsByName.get(requiredJobName);
            job.dependencyMap.put(requiredJob, condition);
            requiredJob.requiredConditions.add(condition);
        });
    }

    private void initializeJobDependencyLevel(Job job) {
        int dependencyLevel = dependencyLevelOf(job, new ArrayList<>());
        for (int i = pipeline.dependencyLevels.size(); i <= dependencyLevel; i++)
            pipeline.dependencyLevels.add(new HashSet<>());
        Set<Job> dependencyLevelJobs = pipeline.dependencyLevels.get(dependencyLevel);
        dependencyLevelJobs.add(job);
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
        Collections.sort(pipeline.jobs, (job1, job2) -> job1.dependencyLevel().compareTo(job2.dependencyLevel()));
    }

    private void initializeJobStopMode(Job job) {
        if ((job.requiredConditions.contains(DependsOnCondition.SERVICE_STARTED)
                || job.requiredConditions.contains(DependsOnCondition.SERVICE_HEALTHY))
                && !(job.requiredConditions.contains(DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY)
                        || job.requiredConditions.contains(DependsOnCondition.SERVICE_FAILED))) {
            job.autoStopWhenDirectDependentsHaveFinalStatus = true;
            pipeline.autoStopJobs.add(job);
        }
    }
}
