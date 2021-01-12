package me.adarlan.plankton.bash;

public class BashScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BashScriptException(BashScript bashScript, String msg) {
        super(makeMessage(bashScript, msg));
    }

    public BashScriptException(BashScript bashScript, String msg, Throwable e) {
        super(makeMessage(bashScript, msg), e);
    }

    private static String makeMessage(BashScript bashScript, String msg) {
        return BashScript.class.getSimpleName() + ": " + bashScript.getName() + "; " + msg;
    }
}
