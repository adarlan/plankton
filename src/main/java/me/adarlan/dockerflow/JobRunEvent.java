package me.adarlan.dockerflow;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

public class JobRunEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String jobName;

    public JobRunEvent(final String jobName) {
        super(jobName);
        this.jobName = jobName;
    }

    @Override
    public String toString() {
        return "Run " + jobName;
    }
}