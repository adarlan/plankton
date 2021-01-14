package me.adarlan.plankton.core;

public enum ServiceDependencyStatus {
    WAITING, PASSED, BLOCKED;
    // TODO remove this enum
    // the status should be checked on each pipeline iteration
}
