package plankton.util.dockercontainerinspect;

public class DockerInspectException extends RuntimeException {

    public DockerInspectException(String msg, Throwable e) {
        super(msg, e);
    }
}
