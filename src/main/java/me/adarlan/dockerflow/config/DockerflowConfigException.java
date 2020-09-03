package me.adarlan.dockerflow.config;

public class DockerflowConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerflowConfigException(Throwable e) {
        super(e);
    }

    public DockerflowConfigException(String msg) {
        super(msg);
    }

    public DockerflowConfigException(String msg, Throwable e) {
        super(msg, e);
    }
}
