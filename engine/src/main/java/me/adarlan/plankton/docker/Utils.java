package me.adarlan.plankton.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.adarlan.plankton.logging.Colors;

class Utils {

    private Utils() {
    }

    static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    static String infoPrefixOf(ServiceImplementation service) {
        PipelineImplementation pipeline = service.pipeline;
        return prefix(service.color, service.name, pipeline.biggestServiceNameLength, " ", "-", "--| ");
    }

    static String logPrefixOf(ServiceImplementation service) {
        PipelineImplementation pipeline = service.pipeline;
        return prefix(service.color, service.name, pipeline.biggestServiceNameLength, " ", "-", "--| ");
    }

    static String logPrefixOf(ServiceInstanceImplementation instance) {
        ServiceImplementation service = instance.parentService;
        PipelineImplementation pipeline = service.pipeline;
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
