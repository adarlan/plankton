package me.adarlan.plankton.pipeline;

public class PipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    PipelineException(String msg, Throwable e) {
        super(msg, e);
    }
}
