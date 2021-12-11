package plankton.perspectives;

public class PerspectiveException extends RuntimeException {

    public PerspectiveException(String msg) {
        super(msg);
    }

    public PerspectiveException(String msg, Throwable e) {
        super(msg, e);
    }
}
