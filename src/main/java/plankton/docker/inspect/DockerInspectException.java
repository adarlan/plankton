package plankton.docker.inspect;

public class DockerInspectException extends RuntimeException {

    public DockerInspectException(String msg, Throwable e) {
        super(msg, e);
    }
}
