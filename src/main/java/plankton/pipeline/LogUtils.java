package plankton.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtils {

    private static final boolean COLORFUL = true;

    private static int prefixLength = 0;
    private static final List<String> colors = new ArrayList<>();
    private static final Map<String, String> assignedColors = new HashMap<>();

    static void initializePrefixLength(Set<String> strings) {
        for (String string : strings) {
            if (string.length() > prefixLength)
                prefixLength = string.length();
        }
    }

    private static void initializeColors() {
        colors.add(Colors.BRIGHT_CYAN);
        colors.add(Colors.BRIGHT_YELLOW);
        if (COLORFUL)
            colors.add(Colors.BRIGHT_BLUE);
        if (COLORFUL)
            colors.add(Colors.BRIGHT_GREEN);
        colors.add(Colors.BRIGHT_PURPLE);
        if (COLORFUL)
            colors.add(Colors.BRIGHT_RED);
    }

    static String prefixOf(String string) {
        return prefixOf(string, "");
    }

    static String prefixOf(String string1, String string2) {
        int len = (string1 + string2).length();
        String colorizedString1 = colorized(string1);
        StringBuilder sb = new StringBuilder();
        sb.append(colorizedString1);
        sb.append(string2);
        sb.append(" ");
        sb.append(Colors.BRIGHT_BLACK);
        sb.append(" ");
        for (int i = len; i < prefixLength; i++)
            sb.append(" ");
        sb.append(Colors.ANSI_RESET);
        sb.append(" ");
        return sb.toString();
    }

    static String blankPrefix() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < prefixLength; i++)
            sb.append("-");
        sb.append("-- ");
        return sb.toString();
    }

    private static synchronized String colorOf(String key) {
        if (assignedColors.containsKey(key))
            return assignedColors.get(key);
        if (colors.isEmpty())
            initializeColors();
        int colorIndex = assignedColors.keySet().size() % colors.size();
        String color = colors.get(colorIndex);
        assignedColors.put(key, color);
        return color;
    }

    static String colorized(String key) {
        String color = colorOf(key);
        return color + key + Colors.ANSI_RESET;
    }

    static String colorized(String string, String key) {
        String color = colorOf(key);
        return color + string + Colors.ANSI_RESET;
    }
}
