package me.adarlan.plankton.bash;

public class BashScriptFailedException extends BashScriptException {

    private static final long serialVersionUID = 1L;

    BashScriptFailedException(BashScript bashScript, String msg) {
        super(bashScript, msg);
    }
}
