package me.adarlan.plankton.core;

public enum ServiceStatus {
    DISABLED, WAITING, BLOCKED, RUNNING, FAILURE, SUCCESS;

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

    public boolean isFailure() {
        return this == FAILURE;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
