package me.adarlan.plankton.docker;

public class BashScriptException extends PlanktonException {

    private static final long serialVersionUID = 1L;

    public BashScriptException(Throwable e) {
        super(e);
    }

    public BashScriptException(String msg) {
        super(msg);
    }

    public BashScriptException(String msg, Throwable e) {
        super(msg, e);
    }
}
