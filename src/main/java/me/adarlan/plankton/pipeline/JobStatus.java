package me.adarlan.plankton.pipeline;

public enum JobStatus {
    DISABLED, WAITING, BLOCKED, RUNNING, FAILED, SUCCEEDED;

    public boolean isDisabled() {
        return this == DISABLED;
    }

    public boolean isWaiting() {
        return this == WAITING;
    }

    public boolean isBlocked() {
        return this == BLOCKED;
    }

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isSucceeded() {
        return this == SUCCEEDED;
    }
}
