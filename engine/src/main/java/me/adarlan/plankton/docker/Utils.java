package me.adarlan.plankton.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Utils {

    private Utils() {
    }

    public static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }
}
