package me.adarlan.dockerflow;

public class DockerflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerflowException(Throwable e) {
        super(e);
    }

    public DockerflowException(String msg) {
        super(msg);
    }

}
