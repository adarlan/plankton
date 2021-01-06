package me.adarlan.plankton;

public class PlanktonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PlanktonException(Throwable e) {
        super(e);
        Logger.getLogger().fatal(() -> e.getClass().getName() + " -> " + e.getMessage());
    }

    public PlanktonException(String msg) {
        super(msg);
        Logger.getLogger().fatal(() -> msg);
    }

    public PlanktonException(String msg, Throwable e) {
        super(msg, e);
        Logger.getLogger().fatal(() -> msg + ": " + e.getClass().getName() + " -> " + e.getMessage());
    }
}
