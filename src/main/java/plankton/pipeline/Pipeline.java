package plankton.pipeline;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import plankton.compose.ComposeDocument;
import plankton.util.Colors;

@EqualsAndHashCode(of = "composeDocument")
public class Pipeline {

    ComposeDocument composeDocument;
    ContainerRuntimeAdapter containerRuntimeAdapter;

    final List<Job> jobs = new ArrayList<>();
    final Map<String, Job> jobsByName = new HashMap<>();

    List<Set<Job>> dependencyLevels = new ArrayList<>();
    Duration timeoutLimitForJobs;
    final Set<Job> autoStopJobs = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);
    private String logPrefix;

    Pipeline() {
        super();
    }

    void initializeLogPrefix() {
        logPrefix = LogUtils.blankPrefix();
    }

    public void start() {
        logger.info("{}{}PIPELINE_STARTED{}", logPrefix, Colors.BLUE, Colors.ANSI_RESET);
        if (jobs.isEmpty())
            logger.warn("{} ... {}", this, "There are no jobs to run");
        jobs.forEach(Job::start);
    }

    private boolean finished = false;

    synchronized void refresh() {
        if (finished)
            return;
        autoStopJobs();
        refreshFinished();
        if (finished)
            logger.info("{}{}PIPELINE_FINISHED{}", logPrefix, Colors.BLUE, Colors.ANSI_RESET);
    }

    private void autoStopJobs() {
        autoStopJobs.forEach(job -> {
            boolean autoStopNow = true;
            for (Job dependentJob : job.dependents.keySet())
                if (!dependentJob.status.isFinal())
                    autoStopNow = false;
            if (autoStopNow) {
                logger.debug("Auto stopping {} because it is not required anymore", job);
                job.stop();
            }
        });
    }

    private void refreshFinished() {
        boolean f = true;
        for (Job job : jobs) {
            if (!job.status.isFinal())
                f = false;
        }
        finished = f;
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

    @Override
    public String toString() {
        return Pipeline.class.getSimpleName();
    }
}
