package plankton.pipeline;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = { "pipeline", "job", "index" })
public class JobInstance {

    Pipeline pipeline;
    Job job;
    int index;

    private boolean running = false;
    private boolean exited = false;
    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;
    private Integer exitCode = null;

    String colorizedName;
    String logPrefix;

    private static final Logger logger = LoggerFactory.getLogger(JobInstance.class);

    JobInstance() {
        super();
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
        exitCode = pipeline.containerRuntimeAdapter.runContainerAndGetExitCode(job.composeService, index);
        finalInstant = Instant.now();
        duration = Duration.between(initialInstant, finalInstant);
        running = false;
        exited = true;
        job.exited(this);
    }

    void stop() {
        synchronized (this) {
            logger.debug("{}Stopping instance", logPrefix);
            pipeline.containerRuntimeAdapter.stopContainer(job.composeService, index);
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
