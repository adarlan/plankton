package me.adarlan.dockerflow;

public class DockerflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerflowException(Throwable e) {
        super(e);
        Logger.getLogger().fatal(() -> e.getClass().getName() + " -> " + e.getMessage());
    }

    public DockerflowException(String msg) {
        super(msg);
        Logger.getLogger().fatal(() -> msg);
    }

    public DockerflowException(String msg, Throwable e) {
        super(msg, e);
        Logger.getLogger().fatal(() -> msg + ": " + e.getClass().getName() + " -> " + e.getMessage());
    }
}
