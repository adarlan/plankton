package me.adarlan.plankton.util;

public class FileSystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileSystemException(String msg, Throwable e) {
        super(msg, e);
    }
}
