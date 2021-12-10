package plankton.bash;

public class BashScriptFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    BashScriptFailedException(int exitCode) {
        super("Exit code: " + exitCode);
    }
}
