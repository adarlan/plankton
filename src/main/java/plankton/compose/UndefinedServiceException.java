package plankton.compose;

public class UndefinedServiceException extends RuntimeException {

    UndefinedServiceException(String msg) {
        super(msg);
    }
}
