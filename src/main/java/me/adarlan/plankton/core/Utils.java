package me.adarlan.plankton.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.adarlan.plankton.util.Colors;

class Utils {

    private Utils() {
    }

    static boolean stringMatchesRegex(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    static String prefixOf(Job job) {
        Pipeline pipeline = job.pipeline;
        return prefix(job.color, job.name, pipeline.biggestJobNameLength, " ", "-", "--| ");
    }

    static String prefixOf(JobInstance instance) {
        Job job = instance.job;
        Pipeline pipeline = job.pipeline;
        return prefix(job.color, instance.name, pipeline.biggestJobNameLength, " ", "-", "--| ");
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
