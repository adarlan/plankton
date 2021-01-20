package me.adarlan.plankton.pipeline;

public class JobNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    JobNotFoundException(String jobName) {
        super("Job not found: " + jobName);
    }
}
