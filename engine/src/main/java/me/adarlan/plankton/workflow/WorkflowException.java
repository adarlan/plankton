package me.adarlan.plankton.workflow;

public class WorkflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    WorkflowException(String msg, Throwable e) {
        super(msg, e);
    }
}
