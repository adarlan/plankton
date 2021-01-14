package me.adarlan.plankton.compose;

public class ComposeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ComposeException(String msg) {
        super(msg);
    }

    public ComposeException(String msg, Throwable e) {
        super(msg, e);
    }
}
