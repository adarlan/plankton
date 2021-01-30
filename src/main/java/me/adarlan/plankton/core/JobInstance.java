package me.adarlan.plankton.core;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.util.Colors;

@EqualsAndHashCode(of = { "job", "index" })
public class JobInstance {

    final Pipeline pipeline;
    final Job job;
    final int index;

    final ComposeDocument compose;
    final ContainerRuntimeAdapter adapter;
    final ComposeService service;

    private boolean running = false;
    private boolean exited = false;

    private Instant initialInstant = null;
    private Instant finalInstant = null;
    private Duration duration = null;

    private Integer exitCode = null;

    private static final Logger logger = LoggerFactory.getLogger(JobInstance.class);
    final String colorizedName;

    JobInstance(Job job, int index) {
        this.job = job;
        this.pipeline = job.pipeline;
        this.index = index;
        this.compose = job.compose;
        this.adapter = job.adapter;
        this.service = job.service;

        if (job.scale() == 1) {
            colorizedName = Colors.colorized(job.name);
        } else {
            colorizedName = Colors.colorized(job.name + "_" + index, job.name);
        }
    }

    @Override
    public String toString() {
        return colorizedName;
    }

    void start() {
        logger.debug("{} ... Starting instance", this);
        Thread thread = new Thread(this::run);
        thread.setUncaughtExceptionHandler((t, e) -> {
            throw new PipelineException(this, "Unable to start", e);
        });
        thread.start();
    }

    private void run() {
        initialInstant = Instant.now();
        running = true;
        exitCode = adapter.runContainerAndGetExitCode(service, index);
        finalInstant = Instant.now();
        duration = Duration.between(initialInstant, finalInstant);
        running = false;
        exited = true;
        job.refresh();
    }

    void stop() {
        synchronized (this) {
            logger.debug("{} ... Stopping instance", this);
            adapter.stopContainer(service, index);
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
