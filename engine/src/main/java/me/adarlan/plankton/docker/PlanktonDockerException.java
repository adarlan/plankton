package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.Logger;

public class PlanktonDockerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlanktonDockerException(Throwable e) {
        super(e);
        Logger.getLogger().fatal(() -> e.getClass().getName() + " -> " + e.getMessage());
    }

    public PlanktonDockerException(String msg) {
        super(msg);
        Logger.getLogger().fatal(() -> msg);
    }

    public PlanktonDockerException(String msg, Throwable e) {
        super(msg, e);
        Logger.getLogger().fatal(() -> msg + ": " + e.getClass().getName() + " -> " + e.getMessage());
    }
}
