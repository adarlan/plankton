package plankton.util;

public class BashScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    BashScriptException(String msg, Throwable e) {
        super(msg, e);
    }
}
