package me.adarlan.plankton.bash;

public class BashScriptFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    BashScriptFailedException() {
        super();
    }

    public BashScriptFailedException(String msg) {
        super(msg);
    }

    public BashScriptFailedException(String msg, Throwable e) {
        super(msg, e);
    }
}
