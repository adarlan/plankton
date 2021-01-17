package me.adarlan.plankton.compose;

public class ComposeDocumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ComposeDocumentException(String msg, Throwable e) {
        super(msg, e);
    }
}
