package plankton.core;

public class JobDependencyLoopException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    JobDependencyLoopException(String msg) {
        super(msg);
    }
}
