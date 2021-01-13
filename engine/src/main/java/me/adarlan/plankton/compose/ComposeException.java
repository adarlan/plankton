package me.adarlan.plankton.compose;

import me.adarlan.plankton.logging.Logger;

public class ComposeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ComposeException(String msg) {
        super(msg);
        Logger.getLogger().fatal(() -> msg);
    }

    public ComposeException(String msg, Throwable e) {
        super(msg, e);
        Logger.getLogger().fatal(() -> msg + "; " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
