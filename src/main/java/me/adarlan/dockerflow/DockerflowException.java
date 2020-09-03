package me.adarlan.dockerflow;

public class DockerflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerflowException(Throwable e) {
        super(e);
        Logger.fatal(() -> e.getClass().getName() + " >> " + e.getMessage());
    }

    public DockerflowException(String msg) {
        super(msg);
        Logger.fatal(() -> msg);
    }

    public DockerflowException(String msg, Throwable e) {
        super(msg, e);
        Logger.fatal(() -> msg + ": " + e.getClass().getName() + " >> " + e.getMessage());
    }
}
