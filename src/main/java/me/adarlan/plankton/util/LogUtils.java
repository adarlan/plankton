package me.adarlan.plankton.util;

import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtils {

    private static int prefixLength = 0;

    public static void initializePrefixLength(Set<String> strings) {
        for (String string : strings) {
            if (string.length() > prefixLength)
                prefixLength = string.length();
        }
    }

    public static String prefixOf(String string) {
        return prefixOf(string, "");
    }

    public static String prefixOf(String string1, String string2) {
        int len = (string1 + string2).length();
        String colorizedString1 = Colors.colorized(string1);
        StringBuilder sb = new StringBuilder();
        sb.append(colorizedString1);
        sb.append(string2);
        sb.append(" ");
        sb.append(Colors.BRIGHT_BLACK);
        sb.append("...");
        for (int i = len; i < prefixLength; i++)
            sb.append(".");
        sb.append(Colors.ANSI_RESET);
        sb.append(" ");
        return sb.toString();
    }

    // static String prefixOf(Job job) {
    // Pipeline pipeline = job.pipeline;
    // return prefix(job.color, job.name, pipeline.biggestJobNameLength, " ", "-",
    // "--| ");
    // }

    // static String prefixOf(JobInstance instance) {
    // Job job = instance.job;
    // Pipeline pipeline = job.pipeline;
    // return prefix(job.color, instance.name, pipeline.biggestJobNameLength, " ",
    // "-", "--| ");
    // }

    // private static String prefix(String color, String name, int
    // biggestNameLength, String startWith, String fillWith,
    // String endWith) {
    // StringBuilder sb = new StringBuilder();
    // sb.append(color);
    // sb.append(name);
    // sb.append(Colors.ANSI_RESET);
    // sb.append(Colors.BRIGHT_BLACK);
    // sb.append(startWith);
    // for (int i = name.length(); i < biggestNameLength; i++)
    // sb.append(fillWith);
    // sb.append(endWith);
    // sb.append(Colors.ANSI_RESET);
    // return sb.toString();
    // }
}
