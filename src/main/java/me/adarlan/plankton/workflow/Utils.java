package me.adarlan.plankton.workflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Utils {

    private Utils() {
    }

    static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    static String prefixOf(Service service) {
        Pipeline pipeline = service.pipeline;
        return prefix(service.color, service.name, pipeline.biggestServiceNameLength, " ", "-", "--| ");
    }

    static String prefixOf(ServiceInstance instance) {
        Service service = instance.parentService;
        Pipeline pipeline = service.pipeline;
        return prefix(service.color, instance.name, pipeline.biggestServiceNameLength, " ", "-", "--| ");
    }

    private static String prefix(String color, String name, int biggestNameLength, String startWith, String fillWith,
            String endWith) {
        StringBuilder sb = new StringBuilder();
        sb.append(color);
        sb.append(name);
        sb.append(Colors.ANSI_RESET);
        sb.append(Colors.BRIGHT_BLACK);
        sb.append(startWith);
        for (int i = name.length(); i < biggestNameLength; i++)
            sb.append(fillWith);
        sb.append(endWith);
        sb.append(Colors.ANSI_RESET);
        return sb.toString();
    }
}
