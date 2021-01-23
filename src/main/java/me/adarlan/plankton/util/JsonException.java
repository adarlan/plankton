package me.adarlan.plankton.util;

public class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    JsonException(String msg, Throwable e) {
        super(msg, e);
    }
}
