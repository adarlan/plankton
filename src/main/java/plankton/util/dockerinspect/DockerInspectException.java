package plankton.util.dockerinspect;

public class DockerInspectException extends RuntimeException {

    public DockerInspectException(String msg, Throwable e) {
        super(msg, e);
    }
}
