package me.adarlan.plankton.runner;

public class PlanktonRunnerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlanktonRunnerException(String msg, Throwable e) {
        super(msg, e);
    }
}
