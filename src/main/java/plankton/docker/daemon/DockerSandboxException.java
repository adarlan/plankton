package plankton.docker.daemon;

public class DockerSandboxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DockerSandboxException(String msg, Throwable e) {
        super(msg, e);
    }
}
