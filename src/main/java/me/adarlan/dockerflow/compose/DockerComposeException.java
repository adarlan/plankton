package me.adarlan.dockerflow.compose;

public class DockerComposeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerComposeException(Throwable e) {
        super(e);
    }

    public DockerComposeException(String msg) {
        super(msg);
    }

    public DockerComposeException(String msg, Throwable e) {
        super(msg, e);
    }
}
