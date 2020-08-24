package me.adarlan.dockerflow;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

public class JobCancelEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String jobName;

    public JobCancelEvent(final String jobName) {
        super(jobName);
        this.jobName = jobName;
    }

    @Override
    public String toString() {
        return "Cancel " + jobName;
    }
}