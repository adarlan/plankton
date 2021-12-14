package plankton.compose;

public enum DependsOnCondition {
    SERVICE_STARTED, SERVICE_HEALTHY, SERVICE_COMPLETED_SUCCESSFULLY, SERVICE_FAILED;

    static DependsOnCondition of(String string) {
        switch (string) {
            case "service_started":
                return SERVICE_STARTED;
            case "service_healthy":
                return SERVICE_HEALTHY;
            case "service_completed_successfully":
                return SERVICE_COMPLETED_SUCCESSFULLY;
            case "service_failed":
                return SERVICE_FAILED;
            default:
                throw new ComposeFormatException(
                        "Unexpected " + DependsOnCondition.class.getSimpleName() + ": " + string);
        }
    }
}
