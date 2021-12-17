package plankton.pipeline;

import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.compose.ComposeDocument;
import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;

public class PipelineInitializer {

    private final ComposeDocument composeDocument;

    private final Set<String> target;
    private final Set<String> skip;
    private final Set<String> resolvedTargetNames = new HashSet<>();
    private final Set<String> resolvedSkipNames = new HashSet<>();

    private final Pipeline pipeline;

    private static final Logger logger = LoggerFactory.getLogger(PipelineInitializer.class);

    public PipelineInitializer(PipelineConfiguration config) {

        composeDocument = config.composeDocument();
        target = config.targetJobs();
        skip = config.skipJobs();

        pipeline = new Pipeline();
        pipeline.composeDocument = config.composeDocument();
        pipeline.containerRuntimeAdapter = config.containerRuntimeAdapter();
        pipeline.timeoutLimitForJobs = config.timeoutLimitForJobs();

        instantiateJobs();

        initializeJobsDependencies();
        resolveJobsDependencies();
        removeDependenciesOnAbstractJobs();

        resolveTargetNames();
        electTargetJobsAndItsDependencies();

        resolveSkipNames();
        unelectSkippedJobs();
        removeDependenciesOnSkippedJobs();

        removeAbstractAndNonElectedJobs();

        pipeline.jobs.forEach(this::initializeJobInstances);
        pipeline.jobs.forEach(this::initializeJobDependencyLevel);
        sortJobsByDependencyLevel();
        pipeline.jobs.forEach(this::initializeJobStopMode);

        initializeLogPrefixLength();
        initializeColorizedNamesAndLogPrefixes();

        pipeline.jobs.forEach(job -> {
            logger.info("{}", job);
            logger.info("  - Dependency level: {}", job.dependencyLevel);
            job.dependencies.forEach(
                    (dependencyJob, condition) -> logger.info("  - Depends on {} with status {}", dependencyJob,
                            condition));
        });
    }

    private void instantiateJobs() {
        composeDocument.services().forEach(service -> {
            Job job = new Job();
            job.pipeline = pipeline;
            job.composeService = service;
            job.name = service.name();
            addJob(job);
        });
    }

    private void initializeJobsDependencies() {
        pipeline.jobs.forEach(this::initializeJobDependencies);
    }

    private void initializeJobDependencies(Job job) {
        logger.debug("Initializing {} dependencies", job);
        ComposeService service = job.composeService;
        service.dependsOn().forEach((dependencyService, condition) -> {
            Job dependencyJob = pipeline.jobsByName.get(dependencyService.name());
            job.dependencies.put(dependencyJob, condition);
            dependencyJob.dependents.put(job, condition);
            logger.debug("  - Initialized dependency: {} depends on {} with condition {}", job, dependencyJob,
                    condition);
        });
    }

    private void resolveJobsDependencies() {
        pipeline.jobs.forEach(job -> {
            if (!job.dependencies.isEmpty()) {
                logger.debug("Resolving {} dependencies", job);
                resolveJobDependencies(job);
            }
        });
    }

    private void resolveJobDependencies(Job job) {
        new HashMap<>(job.dependencies)
                .forEach((dependencyJob, condition) -> resolveJobDependency(job, dependencyJob, condition));
    }

    private void resolveJobDependency(Job job, Job dependencyJob, DependsOnCondition condition) {
        Set<ComposeService> childServices = dependencyJob.composeService.childServices();
        if (!childServices.isEmpty()) {
            childServices.forEach(childService -> {
                Job dependencyJobChild = pipeline.jobsByName.get(childService.name());
                logger.debug(
                        "  - Forwarding {} dependency resolution from {} to its child {}", job, dependencyJob,
                        dependencyJobChild);
                resolveJobDependency(job, dependencyJobChild, condition);
            });
        }
        if (dependencyJob.name.startsWith(".")) {
            dependencyJob.dependencies.forEach((j, c) -> {
                logger.debug(
                        "  - Forwarding {} dependency resolution from {} to its dependency {}", job, dependencyJob, j);
                resolveJobDependency(job, j, mostRelevant(condition, c));
            });
        } else {
            if (job.dependencies.containsKey(dependencyJob)) {
                DependsOnCondition previousCondition = job.dependencies.get(dependencyJob);
                if (ambiguity(condition, previousCondition))
                    throw new DependencyAmbiguityException(
                            job.name + " has conflicting dependencies on " + dependencyJob.name
                                    + " (" + condition + " and " + previousCondition + ")");
                else if (condition.relevance() > previousCondition.relevance()) {
                    job.dependencies.put(dependencyJob, condition);
                    dependencyJob.dependents.put(job, condition);
                    logger.debug(
                            "  - Overridden dependency: {} depends on {} with condition {} (most relevant than {})",
                            job, dependencyJob, condition, previousCondition);
                }
            } else {
                job.dependencies.put(dependencyJob, condition);
                dependencyJob.dependents.put(job, condition);
                logger.debug("  - Resolved dependency: {} depends on {} with condition {}", job, dependencyJob,
                        condition);
            }
        }
    }

    private boolean ambiguity(DependsOnCondition c1, DependsOnCondition c2) {
        Set<DependsOnCondition> set = new HashSet<>();
        set.add(c1);
        set.add(c2);
        return set.contains(DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY)
                && set.contains(DependsOnCondition.SERVICE_FAILED);
    }

    private DependsOnCondition mostRelevant(DependsOnCondition c1, DependsOnCondition c2) {
        return c1.relevance() > c2.relevance()
                ? c1
                : c2;
    }

    private void removeDependenciesOnAbstractJobs() {
        pipeline.jobs.forEach(job -> new HashMap<>(job.dependencies).forEach((dependencyJob, condition) -> {
            if (dependencyJob.name.startsWith(".")) {
                job.dependencies.remove(dependencyJob);
                logger.debug("Removed abstract dependency: {} -> {}", job, dependencyJob);
            }
        }));
    }

    private void resolveTargetNames() {
        if (target.isEmpty()) {
            pipeline.jobs.forEach(job -> {
                if (!job.name.startsWith("."))
                    resolvedTargetNames.add(job.name);
            });
        } else {
            target.forEach(name -> {
                if (name.startsWith(".")) {
                    ComposeService service = composeDocument.getServiceByName(name);
                    service.childServices().forEach(childService -> {
                        if (!childService.name().startsWith("."))
                            resolvedTargetNames.add(childService.name());
                    });
                } else {
                    resolvedTargetNames.add(name);
                }
            });
        }
        logger.debug("Resolved target names: {}", resolvedTargetNames);
    }

    private void electTargetJobsAndItsDependencies() {
        resolvedTargetNames.forEach(name -> {
            Job job = pipeline.jobsByName.get(name);
            electJobAndItsDependencies(job);
        });
    }

    private void electJobAndItsDependencies(Job job) {
        if (!job.elected) {
            job.elected = true;
            logger.debug("Elected job: {}", job);
            job.dependencies.forEach((dependencyJob, condition) -> electJobAndItsDependencies(dependencyJob));
        }
    }

    private void resolveSkipNames() {
        if (!skip.isEmpty()) {
            skip.forEach(name -> {
                if (name.startsWith(".")) {
                    ComposeService service = composeDocument.getServiceByName(name);
                    service.childServices().forEach(childService -> {
                        if (!childService.name().startsWith("."))
                            resolvedSkipNames.add(childService.name());
                    });
                } else {
                    resolvedSkipNames.add(name);
                }
            });
        }
        logger.debug("Resolved skip names: {}", resolvedSkipNames);
    }

    private void unelectSkippedJobs() {
        resolvedSkipNames.forEach(name -> {
            Job job = pipeline.jobsByName.get(name);
            job.elected = false;
            logger.debug("Skipped job: {}", job);
        });
    }

    private void removeDependenciesOnSkippedJobs() {
        pipeline.jobs.forEach(job -> new HashMap<>(job.dependencies).forEach((dependencyJob, condition) -> {
            if (resolvedSkipNames.contains(dependencyJob.name)) {
                job.dependencies.remove(dependencyJob);
                logger.debug("Removed skipped dependency: {} -> {}", job, dependencyJob);
            }
        }));
    }

    private void removeAbstractAndNonElectedJobs() {
        new HashSet<>(pipeline.jobs).forEach(job -> {
            if (job.name.startsWith(".")) {
                removeJob(job);
                logger.debug("Removed abstract job: {}", job);
            } else if (!job.elected) {
                removeJob(job);
                logger.debug("Removed non elected job: {}", job);
            }
        });
    }

    private void addJob(Job job) {
        pipeline.jobs.add(job);
        pipeline.jobsByName.put(job.name, job);
    }

    private void removeJob(Job job) {
        pipeline.jobs.remove(job);
        pipeline.jobsByName.remove(job.name);
    }

    public Pipeline pipeline() {
        return pipeline;
    }

    private void initializeJobInstances(Job job) {
        for (int instanceIndex = 0; instanceIndex < job.composeService.scale(); instanceIndex++) {
            JobInstance instance = new JobInstance();
            instance.job = job;
            instance.pipeline = job.pipeline;
            instance.index = instanceIndex;
            job.instances.add(instance);
        }
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
            for (Job requiredJob : job.dependencies.keySet()) {
                if (knownDependents.contains(requiredJob)) {
                    throw new JobDependencyLoopException(
                            requiredJob + " is one of its dependents: " + knownDependents.toString());
                }
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
        Collection<DependsOnCondition> requiredConditions = job.dependents.values();
        if ((requiredConditions.contains(DependsOnCondition.SERVICE_STARTED)
                || requiredConditions.contains(DependsOnCondition.SERVICE_HEALTHY))
                && !(requiredConditions.contains(DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY)
                        || requiredConditions.contains(DependsOnCondition.SERVICE_FAILED))) {
            job.autoStopWhenDirectDependentsHaveFinalStatus = true;
            pipeline.autoStopJobs.add(job);
        }
    }

    private void initializeLogPrefixLength() {
        Set<String> ns = new HashSet<>();
        pipeline.jobs.forEach(job -> {
            ComposeService service = job.composeService;
            ns.add(service.scale() > 1
                    ? service.name() + "[" + service.scale() + "]"
                    : service.name());
        });
        LogUtils.initializePrefixLength(ns);
    }

    private void initializeColorizedNamesAndLogPrefixes() {
        pipeline.initializeLogPrefix();
        pipeline.jobs.forEach(job -> {
            job.initializeColorizedNameAndLogPlaceholders();
            job.instances.forEach(JobInstance::initializeColorizedNameAndLogPlaceholders);
        });
    }
}
