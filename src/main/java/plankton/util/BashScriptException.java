package plankton.util;

public class BashScriptException extends RuntimeException {

    BashScriptException(String msg, Throwable e) {
        super(msg, e);
    }
}
