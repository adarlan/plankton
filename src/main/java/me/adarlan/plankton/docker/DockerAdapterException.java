package me.adarlan.plankton.docker;

public class DockerAdapterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DockerAdapterException(String msg, Throwable e) {
        super(msg, e);
    }
}
