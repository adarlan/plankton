package plankton.setup;

public class PlanktonSetupException extends RuntimeException {

    PlanktonSetupException(String msg, Throwable e) {
        super(msg, e);
    }

    PlanktonSetupException(String msg) {
        super(msg);
    }
}
