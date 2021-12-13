package plankton;

public class PlanktonSetupException extends RuntimeException {

    public PlanktonSetupException(String msg, Throwable e) {
        super(msg, e);
    }

    public PlanktonSetupException(String msg) {
        super(msg);
    }
}
