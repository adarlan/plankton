package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.Logger;
import me.adarlan.plankton.util.BashScript;

class Utils {

    private Utils() {
    }

    static BashScript createScript(String name, Logger logger) {
        BashScript script = new BashScript(name);
        script.forEachVariable(variable -> logger.debug(() -> script.getName() + " | " + variable));
        script.forEachCommand(command -> logger.debug(() -> script.getName() + " | " + command));
        script.forEachOutput(output -> logger.debug(() -> script.getName() + " >> " + output));
        script.forEachError(error -> logger.error(() -> script.getName() + " >> " + error));
        return script;
    }
}