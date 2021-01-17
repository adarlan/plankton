package me.adarlan.plankton.docker;

public class DockerComposeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DockerComposeException(String msg, Throwable e) {
        super(msg, e);
    }
}
