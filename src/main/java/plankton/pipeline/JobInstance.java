package plankton.pipeline;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import plankton.util.Colors;

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

    private String colorizedName;
    private String infoPlaceholder;
    private String errorPlaceholder;
    private static final Logger logger = LoggerFactory.getLogger(JobInstance.class);

    JobInstance() {
        super();
    }

    void initializeColorizedNameAndLogPlaceholders() {
        String prefix;
        if (job.composeService.scale() > 1) {
            colorizedName = LogUtils.colorized(job.name + "_" + index, job.name);
            prefix = LogUtils.prefixOf(job.name, "[" + index + "]");
        } else {
            colorizedName = LogUtils.colorized(job.name);
            prefix = LogUtils.prefixOf(job.name);
        }
        infoPlaceholder = prefix
                + Colors.BLUE + "INFO  " + Colors.ANSI_RESET + "{}";
        errorPlaceholder = prefix
                + Colors.RED + "ERROR " + Colors.ANSI_RESET + "{}";
    }

    @Override
    public String toString() {
        return colorizedName == null
                ? job.name + "[" + index + "]"
                : colorizedName;
    }

    void start() {
        logger.debug("Starting instance: {}", this);
        Thread thread = new Thread(this::run);
        thread.setUncaughtExceptionHandler(
                (t, e) -> job.setFinalStatusError(e.getClass().getSimpleName() + ": " + e.getMessage()));
        thread.start();
    }

    private void run() {
        initialInstant = Instant.now();
        running = true;
        exitCode = pipeline.containerRuntimeAdapter
                .startContainerAndGetExitCode(ContainerConfiguration.builder()
                        .service(job.composeService)
                        .index(index)
                        .forEachOutput(msg -> logger.info(infoPlaceholder, msg))
                        .forEachError(msg -> logger.error(errorPlaceholder, msg))
                        .build());
        finalInstant = Instant.now();
        duration = Duration.between(initialInstant, finalInstant);
        running = false;
        exited = true;
        job.exited(this);
    }

    void stop() {
        synchronized (this) {
            logger.debug("Stopping instance: {}", this);
            pipeline.containerRuntimeAdapter
                    .stopContainer(ContainerConfiguration.builder()
                            .service(job.composeService)
                            .index(index)
                            .forEachOutput(msg -> logger.info(
                                    "{}STOPPING_CONTAINER{}Index: {}",
                                    job.redLabel, job.separator, index))
                            .forEachError(msg -> logger.error(
                                    "{}STOPPING_CONTAINER{}Index: {}",
                                    job.redLabel, job.separator, index))
                            .build());
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
