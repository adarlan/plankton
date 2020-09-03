package me.adarlan.dockerflow;

import lombok.Getter;

public class JobFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String jobName;

    public JobFailedException(String jobName, String msg) {
        super(msg);
        this.jobName = jobName;
    }

    public JobFailedException(String jobName, String msg, Throwable e) {
        super(msg, e);
        this.jobName = jobName;
    }
}
