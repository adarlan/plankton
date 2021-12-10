package plankton.core;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;

import plankton.compose.ComposeDocument;
import plankton.compose.ComposeService;
import plankton.util.Colors;
import plankton.util.LogUtils;

@EqualsAndHashCode(of = { "job", "index" })
public class JobInstance {

    final Pipeline pipeline;
    final Job job;
    final int index;

    final ComposeDocument composeDocument;
    final ComposeService composeService;
    final ContainerRuntimeAdapter containerRuntimeAdapter;

    private boolean running = false;
    private boolean exited = false;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;

    private Integer exitCode = null;

    private static final Logger logger = LoggerFactory.getLogger(JobInstance.class);
    private final String colorizedName;
    private final String logPrefix;

    JobInstance(Job job, int index) {
        this.job = job;
        this.pipeline = job.pipeline;
        this.index = index;
        this.composeDocument = job.composeDocument;
        this.containerRuntimeAdapter = job.containerRuntimeAdapter;
        this.composeService = job.composeService;

        if (composeService.scale() > 1) {
            colorizedName = Colors.colorized(job.name + "_" + index, job.name);
            logPrefix = LogUtils.prefixOf(job.name, "[" + index + "]");
        } else {
            colorizedName = Colors.colorized(job.name);
            logPrefix = LogUtils.prefixOf(job.name);
        }
    }

    @Override
    public String toString() {
        return colorizedName;
    }

    void start() {
        logger.debug("{}Starting instance", logPrefix);
        Thread thread = new Thread(this::run);
        thread.setUncaughtExceptionHandler(
                (t, e) -> job.setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage()));
        thread.start();
    }

    private void run() {
        initialInstant = Instant.now();
        running = true;
        exitCode = containerRuntimeAdapter.runContainerAndGetExitCode(composeService, index);
        finalInstant = Instant.now();
        duration = Duration.between(initialInstant, finalInstant);
        running = false;
        exited = true;
        job.exited(this);
    }

    void stop() {
        synchronized (this) {
            logger.debug("{}Stopping instance", logPrefix);
            containerRuntimeAdapter.stopContainer(composeService, index);
        }
    }

    public Duration duration() {
        synchronized (this) {
            if (running)
                return Duration.between(initialInstant, Instant.now());
            else if (exited)
                return duration;
            else
                return Duration.ZERO;
        }
    }

    public boolean running() {
        return running;
    }

    public boolean exited() {
        return exited;
    }

    public Job job() {
        return job;
    }

    public int index() {
        return index;
    }

    public Instant initialInstant() {
        return initialInstant;
    }

    public Instant finalInstant() {
        return finalInstant;
    }

    public Integer exitCode() {
        return exitCode;
    }
}
