package plankton.pipeline;

public class DependencyAmbiguityException extends RuntimeException {

    DependencyAmbiguityException(String msg) {
        super(msg);
    }
}
