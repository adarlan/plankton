package plankton.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import plankton.compose.ComposeService;
import plankton.compose.DependsOnCondition;

@EqualsAndHashCode(of = { "pipeline", "name" })
public class Job {

    private static final String SUCCEEDED = Colors.GREEN + "SUCCEEDED" + Colors.ANSI_RESET
            + " " + Colors.BRIGHT_BLACK + "..." + Colors.ANSI_RESET + " ";

    private static final String FAILED = Colors.RED + "FAILED" + Colors.ANSI_RESET
            + " " + Colors.BRIGHT_BLACK + "......" + Colors.ANSI_RESET + " ";

    Pipeline pipeline;
    String name;
    boolean elected = false;
    ComposeService composeService;

    final Map<Job, DependsOnCondition> dependencies = new HashMap<>();
    final Map<Job, DependsOnCondition> dependents = new HashMap<>();
    Integer dependencyLevel;
    boolean autoStopWhenDirectDependentsHaveFinalStatus = false;

    private JobStatus status = JobStatus.CREATED;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;

    private Integer exitCode = null;

    private Thread thread = null;

    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    private String colorizedName;
    private String logPrefix;
    private String infoPlaceholder;
    private String errorPlaceholder;

    Job() {
        super();
    }

    public Integer exitCode() {
        return exitCode;
    }

    void initializeColorizedNameAndLogPlaceholders() {
        colorizedName = LogUtils.colorized(name);
        logPrefix = LogUtils.prefixOf(name);
        infoPlaceholder = logPrefix + "{}";
        errorPlaceholder = logPrefix + Colors.RED + "ERROR" + Colors.ANSI_RESET + " {}";
    }

    void start() {
        logger.debug("Starting {}", this);
        thread = new Thread(() -> {
            initialInstant = Instant.now();
            if (composeService.build().isPresent()) {
                status = JobStatus.BUILDING;
                pipeline.containerRuntimeAdapter
                        .buildImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}{}", logPrefix, msg))
                                .forEachError(msg -> logger.error("{}{}", logPrefix, msg))
                                .build());
            } else {
                status = JobStatus.PULLING;
                pipeline.containerRuntimeAdapter
                        .pullImage(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}{}", logPrefix, msg))
                                .forEachError(msg -> logger.error("{}{}", logPrefix, msg))
                                .build());
            }
            if (composeService.build().isPresent() && composeService.image().isPresent()
                    && composeService.entrypointIsReseted() && composeService.command().isEmpty()) {
                status = JobStatus.BUILT;
                finalInstant = Instant.now();
                duration = Duration.between(initialInstant, finalInstant);
                String image = composeService.image().orElseThrow();
                String time = durationAsString();
                logger.info("{}{}Image built: {}; Time: {}", logPrefix, SUCCEEDED, image, time);
                pipeline.notifyJobCompletedSuccessfully(this);
            } else {
                status = JobStatus.RUNNING;
                pipeline.containerRuntimeAdapter.createContainer(ContainerConfiguration.builder()
                        .service(composeService)
                        .forEachOutput(msg -> logger.debug("{}Creating container ... {}", logPrefix, msg))
                        .forEachError(msg -> logger.error("{}Creating container ... {}", logPrefix, msg))
                        .build());
                logger.debug("Starting instance: {}", this);
                exitCode = pipeline.containerRuntimeAdapter
                        .startContainerAndGetExitCode(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info(infoPlaceholder, msg))
                                .forEachError(msg -> logger.error(errorPlaceholder, msg))
                                .build());
                finalInstant = Instant.now();
                duration = Duration.between(initialInstant, finalInstant);
                String time = durationAsString();
                if (exitCode == 0) {
                    status = JobStatus.EXITED_ZERO;
                    logger.info("{}{}Exit code: 0; Time: {}", logPrefix, SUCCEEDED, time);
                    pipeline.notifyJobCompletedSuccessfully(this);
                } else {
                    status = JobStatus.EXITED_NON_ZERO;
                    logger.error("{}{}Exited non-zero code: {}; Time: {}", logPrefix, FAILED, exitCode, time);
                    pipeline.notifyJobFailed(this);
                }
            }
        });
        thread.setUncaughtExceptionHandler(
                (t, e) -> {
                    status = JobStatus.ERROR;
                    if (initialInstant != null) {
                        finalInstant = Instant.now();
                        duration = Duration.between(initialInstant, finalInstant);
                    }
                    logger.error("{}{}An exception was thrown", logPrefix, FAILED, e);
                    stop();
                    pipeline.notifyJobFailed(this);
                });
        thread.start();
    }

    void stop() {
        logger.debug("Stopping job {}", this);
        if (thread != null) {
            synchronized (this) {
                logger.debug("Stopping instance: {}", this);
                // TODO check if container is running
                pipeline.containerRuntimeAdapter
                        .stopContainer(ContainerConfiguration.builder()
                                .service(composeService)
                                .forEachOutput(msg -> logger.info("{}Stopping container ... {}", logPrefix, msg))
                                .forEachError(msg -> logger.error("{}Stopping container ... {}", logPrefix, msg))
                                .build());
            }
            thread.interrupt();
        }
    }

    void block() {
        status = JobStatus.BLOCKED;
        logger.error("{}{}Blocked by dependencies", logPrefix, FAILED);
        pipeline.notifyJobFailed(this);
    }

    private String durationAsString() {
        return duration().getSeconds() + "sec";
    }

    public Duration duration() {
        synchronized (this) {
            if (duration != null)
                return duration;
            else if (initialInstant != null)
                return Duration.between(initialInstant, Instant.now());
            else
                return Duration.ZERO;
        }
    }

    public Map<Job, DependsOnCondition> dependencies() {
        return Collections.unmodifiableMap(dependencies);
    }

    public Pipeline pipeline() {
        return pipeline;
    }

    public String name() {
        return name;
    }

    public JobStatus status() {
        return status;
    }

    public Instant initialInstant() {
        return initialInstant;
    }

    public Instant finalInstant() {
        return finalInstant;
    }

    public Integer dependencyLevel() {
        return dependencyLevel;
    }

    @Override
    public String toString() {
        return colorizedName == null
                ? name
                : colorizedName;
    }
}
