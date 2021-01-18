package me.adarlan.plankton.docker;

public class DockerSandboxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DockerSandboxException(String msg) {
        super(msg);
    }

    DockerSandboxException(String msg, Throwable e) {
        super(msg, e);
    }
}
