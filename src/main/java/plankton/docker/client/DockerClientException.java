package plankton.docker.client;

public class DockerClientException extends RuntimeException {

    public DockerClientException(String msg, Throwable e) {
        super(msg, e);
    }

    public DockerClientException(String msg) {
        super(msg);
    }
}
