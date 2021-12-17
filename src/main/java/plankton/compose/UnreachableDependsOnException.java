package plankton.compose;

public class UnreachableDependsOnException extends RuntimeException {

    UnreachableDependsOnException(String msg) {
        super(msg);
    }
}
