package me.adarlan.plankton.compose;

public enum DependsOnCondition {
    SERVICE_HEALTHY, SERVICE_STARTED, EXIT_ZERO, EXIT_NON_ZERO;

    static DependsOnCondition of(String string) {
        switch (string) {
            case "service_healthy":
                return SERVICE_HEALTHY;
            case "service_started":
                return SERVICE_STARTED;
            case "exit_zero":
                return EXIT_ZERO;
            case "exit_non_zero":
                return EXIT_NON_ZERO;
            default:
                throw new ComposeFileFormatException(
                        "Unexpected " + DependsOnCondition.class.getSimpleName() + ": " + string);
        }
    }
}
