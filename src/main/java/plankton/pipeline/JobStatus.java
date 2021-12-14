package plankton.pipeline;

public enum JobStatus {
    WAITING, PULLING, BUILDING, RUNNING, BLOCKED, ERROR, BUILT, EXITED_ZERO, EXITED_NON_ZERO;

    public boolean isWaiting() {
        return this == WAITING;
    }

    public boolean isPulling() {
        return this == PULLING;
    }

    public boolean isBuilding() {
        return this == BUILDING;
    }

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isBlocked() {
        return this == BLOCKED;
    }

    public boolean isError() {
        return this == ERROR;
    }

    public boolean isBuilt() {
        return this == BUILT;
    }

    public boolean isExitedZero() {
        return this == EXITED_ZERO;
    }

    public boolean isExitedNonZero() {
        return this == EXITED_NON_ZERO;
    }

    public boolean isFailed() {
        return isBlocked() || isError() || isExitedNonZero();
    }

    public boolean isSucceeded() {
        return isBuilt() || isExitedZero();
    }

    public boolean isFinal() {
        return isFailed() || isSucceeded();
    }
}
