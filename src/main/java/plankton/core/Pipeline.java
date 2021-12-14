package plankton.core;

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
import plankton.compose.ComposeService;
import plankton.util.Colors;

@EqualsAndHashCode(of = "composeDocument")
public class Pipeline {

    ComposeDocument composeDocument;
    ContainerRuntimeAdapter containerRuntimeAdapter;
    Set<ComposeService> composeServices;

    final List<Job> jobs = new ArrayList<>();
    final Map<String, Job> jobsByName = new HashMap<>();
    List<Set<Job>> dependencyLevels = new ArrayList<>();
    Duration timeoutLimitForJobs;
    final Set<Job> autoStopJobs = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    Pipeline() {
        super();
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

    private void refreshFinished() {
        boolean f = true;
        for (Job job : jobs) {
            if (!job.status.isFinal())
                f = false;
        }
        finished = f;
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

    @Override
    public String toString() {
        return Pipeline.class.getSimpleName();
    }
}
