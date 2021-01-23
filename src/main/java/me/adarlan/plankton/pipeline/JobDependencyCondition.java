package me.adarlan.plankton.pipeline;

import me.adarlan.plankton.compose.ComposeService;

public enum JobDependencyCondition {

    EXITED_ZERO, EXITED_NON_ZERO;

    // TODO HEALTHY, STARTED

    static JobDependencyCondition of(String string) {
        switch (string) {
            case ComposeService.DependsOn.SERVICE_EXITED_ZERO:
                return EXITED_ZERO;
            case ComposeService.DependsOn.SERVICE_EXITED_NON_ZERO:
                return EXITED_NON_ZERO;
            default:
                return null;
        }
    }

    boolean isSatisfiedFor(Job job) {
        switch (this) {
            case EXITED_ZERO:
                return job.allInstancesExitedZero();
            case EXITED_NON_ZERO:
                return job.anyInstanceExitedNonZero();
            default:
                return false;
        }
    }

    boolean isBlockedFor(Job job) {
        switch (this) {
            case EXITED_ZERO:
                return job.anyInstanceExitedNonZero();
            case EXITED_NON_ZERO:
                return job.allInstancesExitedZero();
            default:
                return false;
        }
    }
}
