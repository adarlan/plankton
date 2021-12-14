package plankton.compose;

public class ComposeFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ComposeFormatException(String msg, Throwable e) {
        super(msg, e);
    }

    ComposeFormatException(String msg) {
        super(msg);
    }
}
