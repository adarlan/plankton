package me.adarlan.dockerflow.bash;

public class BashScriptException extends RuntimeException {

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
