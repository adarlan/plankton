package plankton.docker.api;

public class DockerApiException extends RuntimeException {

    public DockerApiException(String msg, Throwable e) {
        super(msg, e);
    }
}
