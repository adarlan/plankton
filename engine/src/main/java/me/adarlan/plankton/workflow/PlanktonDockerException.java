package me.adarlan.plankton.workflow;

public class PlanktonDockerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlanktonDockerException(String msg) {
        super(msg);
    }

    public PlanktonDockerException(String msg, Throwable e) {
        super(msg, e);
    }
}
