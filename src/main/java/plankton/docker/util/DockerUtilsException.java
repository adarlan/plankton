package plankton.docker.util;

public class DockerUtilsException extends RuntimeException {

    public DockerUtilsException(String msg, Throwable e) {
        super(msg, e);
    }
}
