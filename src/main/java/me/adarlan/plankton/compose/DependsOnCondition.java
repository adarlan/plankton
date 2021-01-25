package me.adarlan.plankton.compose;

public enum DependsOnCondition {
    SERVICE_HEALTHY, SERVICE_STARTED, SERVICE_EXITED_ZERO, SERVICE_EXITED_NON_ZERO;

    static DependsOnCondition of(String string) {
        switch (string) {
            case "service_healthy":
                return SERVICE_HEALTHY;
            case "service_started":
                return SERVICE_STARTED;
            case "service_exited_zero":
                return SERVICE_EXITED_ZERO;
            case "service_exited_non_zero":
                return SERVICE_EXITED_NON_ZERO;
            default:
                throw new ComposeFileFormatException("Unexpected depends_on condition: " + string);
        }
    }
}
