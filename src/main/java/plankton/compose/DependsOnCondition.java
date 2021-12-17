package plankton.compose;

import plankton.util.Colors;

public enum DependsOnCondition {
    SERVICE_STARTED, SERVICE_HEALTHY, SERVICE_COMPLETED_SUCCESSFULLY, SERVICE_FAILED;

    public static DependsOnCondition of(String string) {
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

    public int relevance() {
        switch (this) {
            case SERVICE_STARTED:
                return 1;
            case SERVICE_HEALTHY:
                return 2;
            default:
                return 3;
        }
    }

    // @Override
    // public String toString() {
    // switch (this) {
    // case SERVICE_STARTED:
    // return Colors.YELLOW + "service_started" + Colors.ANSI_RESET;
    // case SERVICE_HEALTHY:
    // return Colors.BLUE + "service_healthy" + Colors.ANSI_RESET;
    // case SERVICE_COMPLETED_SUCCESSFULLY:
    // return Colors.GREEN + "service_completed_successfully" + Colors.ANSI_RESET;
    // case SERVICE_FAILED:
    // return Colors.RED + "service_failed" + Colors.ANSI_RESET;
    // }
    // return super.toString();
    // }
}
