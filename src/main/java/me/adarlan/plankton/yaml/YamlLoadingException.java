package me.adarlan.plankton.yaml;

public class YamlLoadingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    YamlLoadingException(String msg, Throwable e) {
        super(msg, e);
    }
}
