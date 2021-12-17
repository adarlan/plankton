package plankton.compose;

public class CircularDependsOnException extends RuntimeException {

    CircularDependsOnException(String msg) {
        super(msg);
    }
}
