package plankton.docker.bash;

public class BashScriptFailedException extends Exception {

    BashScriptFailedException(int exitCode) {
        super("Exit code: " + exitCode);
    }
}
