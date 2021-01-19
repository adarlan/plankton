package me.adarlan.plankton.bash;

public class BashScriptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    BashScriptException(String msg) {
        super(msg);
    }

    BashScriptException(String msg, Throwable e) {
        super(msg, e);
    }

    BashScriptException(BashScript bashScript, String msg) {
        super(makeMessage(bashScript, msg));
    }

    BashScriptException(BashScript bashScript, String msg, Throwable e) {
        super(makeMessage(bashScript, msg), e);
    }

    private static String makeMessage(BashScript bashScript, String msg) {
        return BashScript.class.getSimpleName() + ": " + bashScript.getName() + "; " + msg;
    }
}
