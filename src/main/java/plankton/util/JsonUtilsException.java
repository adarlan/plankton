package plankton.util;

public class JsonUtilsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    JsonUtilsException(String msg, Throwable e) {
        super(msg, e);
    }
}
