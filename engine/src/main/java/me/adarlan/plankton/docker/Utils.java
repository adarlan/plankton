package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.adarlan.plankton.bash.BashScript;

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

    public static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }
}
