package plankton.pipeline;

import java.time.Duration;
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
import plankton.compose.DependsOnCondition;

@EqualsAndHashCode(of = "composeDocument")
public class Pipeline {

    ComposeDocument composeDocument;
    ContainerRuntimeAdapter containerRuntimeAdapter;

    final List<Job> jobs = new ArrayList<>();
    final Map<String, Job> jobsByName = new HashMap<>();

    List<Set<Job>> dependencyLevels = new ArrayList<>();
    Duration timeoutLimitForJobs;
    final Set<Job> autoStopJobs = new HashSet<>();

    private int jobsRunningLimit = 3;

    private final List<Job> jobsWaitingForDependencies = new ArrayList<>();
    private final List<Job> jobsScheduled = new ArrayList<>();
    private final List<Job> jobsRunning = new ArrayList<>();
    private final List<Job> jobsFinished = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    Pipeline() {
        super();
    }

    public void start() {
        logger.info("Pipeline started");
        initializeQueue();
        updateQueue();
    }

    private void initializeQueue() {
        jobs.forEach(jobsWaitingForDependencies::add);
    }

    private synchronized void updateQueue() {
        new ArrayList<>(jobsWaitingForDependencies).forEach(job -> {
            if (job.dependencies.isEmpty()) {
                jobsWaitingForDependencies.remove(job);
                jobsScheduled.add(job);
                logger.debug("Job scheduled: {}", job);
            }
        });
        while (jobsRunning.size() < jobsRunningLimit && !jobsScheduled.isEmpty()) {
            Job job = jobsScheduled.remove(0);
            jobsRunning.add(job);
            job.start();
            logger.debug("Job started: {}", job);
        }
        updateStatus();
    }

    private void updateStatus() {
        if (jobsFinished.size() == jobs.size()) {
            if (jobsFinished.stream()
                    .filter(job -> job.exitCode() != 0)
                    .collect(Collectors.toList()).isEmpty())
                logger.info("Pipeline completed successfully");
            else
                logger.info("Pipeline failed");
        }
    }

    synchronized void notifyJobStarted(Job job) {
        removeDependenciesOnJob(job, DependsOnCondition.SERVICE_STARTED);
    }

    synchronized void notifyJobHealthy(Job job) {
        removeDependenciesOnJob(job, DependsOnCondition.SERVICE_HEALTHY);
    }

    synchronized void notifyJobFailed(Job job) {
        if (jobsWaitingForDependencies.contains(job))
            jobsWaitingForDependencies.remove(job);
        else
            jobsRunning.remove(job);
        jobsFinished.add(job);
        job.dependents.forEach((dependentJob, condition) -> dependentJob.block());
        autoStopJobs();
    }

    synchronized void notifyJobCompletedSuccessfully(Job job) {
        jobsRunning.remove(job);
        jobsFinished.add(job);
        removeDependenciesOnJob(job, DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY);
        autoStopJobs();
    }

    synchronized void removeDependenciesOnJob(Job job, DependsOnCondition satisfiedCondition) {
        new HashMap<>(job.dependents).forEach((dependentJob, requiredCondition) -> {
            if (satisfiedCondition == requiredCondition) {
                dependentJob.dependencies.remove(job);
                job.dependents.remove(dependentJob);
                logger.debug("Dependency satisfied: {} depends on {} with status {}", dependentJob, job,
                        satisfiedCondition);
            }
        });
        updateQueue();
    }

    private void autoStopJobs() {
        autoStopJobs.forEach(job -> {

            if (job.dependents.isEmpty()) {
                // TODO keep in mind that the dependents are removed...

                logger.debug("Auto stopping job: {}", job);
                job.stop();
            }
        });
    }

    public void stop() {
        logger.debug("Stopping pipeline");
        jobs.forEach(Job::stop);
    }

    public List<Job> jobs() {
        return Collections.unmodifiableList(jobs);
    }

    public Job getJobByName(String jobName) {
        return jobsByName.get(jobName);
    }

    @Override
    public String toString() {
        return Pipeline.class.getSimpleName();
    }
}
