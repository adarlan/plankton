package me.adarlan.plankton.util;

public class YamlException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    YamlException(String msg, Throwable e) {
        super(msg, e);
    }
}
