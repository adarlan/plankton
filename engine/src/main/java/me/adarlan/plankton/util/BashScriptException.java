package me.adarlan.plankton.util;

import me.adarlan.plankton.core.Logger;

public class BashScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BashScriptException(BashScript bashScript, String msg) {
        super(makeMessage(bashScript, msg));
        Logger.getLogger().fatal(() -> makeMessage(bashScript, msg));
        // TODO is it a good practice to log exception messages
        // consider that it is part of the application data
    }

    public BashScriptException(BashScript bashScript, String msg, Throwable e) {
        super(makeMessage(bashScript, msg), e);
        Logger.getLogger().fatal(
                () -> makeMessage(bashScript, msg) + "; " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private static String makeMessage(BashScript bashScript, String msg) {
        return BashScript.class.getSimpleName() + ": " + bashScript.getName() + "; " + msg;
    }
}
