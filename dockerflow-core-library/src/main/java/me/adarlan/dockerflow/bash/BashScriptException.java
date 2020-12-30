package me.adarlan.dockerflow.bash;

import me.adarlan.dockerflow.DockerflowException;

public class BashScriptException extends DockerflowException {

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
