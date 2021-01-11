package me.adarlan.plankton.util;

public class BashScriptFailedException extends BashScriptException {

    private static final long serialVersionUID = 1L;

    public BashScriptFailedException(BashScript bashScript, String msg) {
        super(bashScript, msg);
    }
}
