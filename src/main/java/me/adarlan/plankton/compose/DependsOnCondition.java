package me.adarlan.plankton.compose;

public enum DependsOnCondition {
    SERVICE_HEALTHY, SERVICE_STARTED, SUCCEEDED, FAILED;

    static DependsOnCondition of(String string) {
        switch (string) {
            case "service_healthy":
                return SERVICE_HEALTHY;
            case "service_started":
                return SERVICE_STARTED;
            case "succeeded":
                return SUCCEEDED;
            case "failed":
                return FAILED;
            default:
                throw new ComposeFileFormatException(
                        "Unexpected " + DependsOnCondition.class.getSimpleName() + ": " + string);
        }
    }
}
