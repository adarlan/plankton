package me.adarlan.plankton.compose;

public class ComposeFileFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ComposeFileFormatException(String msg, Throwable e) {
        super(msg, e);
    }

    ComposeFileFormatException(String msg) {
        super(msg);
    }
}
