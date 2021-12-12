package plankton.adapter.docker;

public class DockerAdapterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DockerAdapterException(String msg, Throwable e) {
        super(msg, e);
    }

    DockerAdapterException(String msg) {
        super(msg);
    }
}
