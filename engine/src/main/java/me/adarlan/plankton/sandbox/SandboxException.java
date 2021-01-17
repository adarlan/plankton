package me.adarlan.plankton.sandbox;

public class SandboxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    SandboxException(String msg) {
        super(msg);
    }

    SandboxException(String msg, Throwable e) {
        super(msg, e);
    }
}
