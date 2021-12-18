package plankton.compose;

public enum DependsOnCondition {
    SERVICE_STARTED(1), SERVICE_HEALTHY(2), SERVICE_COMPLETED_SUCCESSFULLY(3);

    private final int relevance;

    private DependsOnCondition(int relevance) {
        this.relevance = relevance;
    }

    public static DependsOnCondition of(String string) {
        switch (string) {
            case "service_started":
                return SERVICE_STARTED;
            case "service_healthy":
                return SERVICE_HEALTHY;
            case "service_completed_successfully":
                return SERVICE_COMPLETED_SUCCESSFULLY;
            default:
                throw new ComposeFormatException(
                        "Unexpected " + DependsOnCondition.class.getSimpleName() + ": " + string);
        }
    }

    public int relevance() {
        return relevance;
    }
}
