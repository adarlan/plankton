package me.adarlan.plankton.pipeline;

public class JobDependencyLoopException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    JobDependencyLoopException() {
        super();
    }
}
