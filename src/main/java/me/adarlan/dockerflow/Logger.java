package me.adarlan.dockerflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Logger {

    private Logger() {
        super();
    }

    public static final String ANSI_RESET = "\u001B[0m";

    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BRIGHT_BLACK = "\u001b[30;1m";
    public static final String BRIGHT_RED = "\u001b[31;1m";
    public static final String BRIGHT_GREEN = "\u001b[32;1m";
    public static final String BRIGHT_YELLOW = "\u001b[33;1m";
    public static final String BRIGHT_BLUE = "\u001b[34;1m";
    public static final String BRIGHT_PURPLE = "\u001b[35;1m";
    public static final String BRIGHT_CYAN = "\u001b[36;1m";
    public static final String BRIGHT_WHITE = "\u001b[37;1m";

    private static final int TRACE = 0;
    private static final int DEBUG = 1;
    private static final int FOLLOW = 2;
    private static final int INFO = 3;
    private static final int WARN = 4;
    private static final int ERROR = 5;
    private static final int FATAL = 6;

    private static int level;

    private static Instant begin;

    public static void init() {
        begin = Instant.now();
        level = FOLLOW;
    }

    private static void follow(Supplier<String> supplier) {
        if (level <= FOLLOW)
            print(FOLLOW, supplier.get());
    }

    public static void info(Supplier<String> supplier) {
        if (level <= INFO)
            print(INFO, supplier.get());
    }

    public static void debug(Supplier<String> supplier) {
        if (level <= DEBUG)
            print(DEBUG, supplier.get());
    }

    public static void error(Supplier<String> supplier) {
        if (level <= ERROR)
            print(ERROR, supplier.get());
    }

    public static void trace(Supplier<String> supplier) {
        if (level <= TRACE)
            print(TRACE, supplier.get());
    }

    public static void fatal(Supplier<String> supplier) {
        if (level <= FATAL)
            print(FATAL, supplier.get());
    }

    private static void print(int LEVEL, String text) {
        Long time = Duration.between(begin, Instant.now()).toMillis();
        String tag = null;
        String tagColor = null;
        String timestamp = time.toString();
        String textColor = null;
        switch (LEVEL) {
            case TRACE:
                tag = "TRACE";
                tagColor = BRIGHT_BLACK;
                textColor = BRIGHT_BLACK;
                break;
            case DEBUG:
                tag = "DEBUG";
                tagColor = BRIGHT_BLACK;
                textColor = BRIGHT_BLACK;
                break;
            case FOLLOW:
                tag = "FOLLOW";
                tagColor = WHITE;
                textColor = WHITE;
                break;
            case INFO:
                tag = "INFO";
                tagColor = WHITE;
                textColor = WHITE;
                break;
            case WARN:
                tag = "WARN";
                tagColor = YELLOW;
                textColor = YELLOW;
                break;
            case ERROR:
                tag = "ERROR";
                tagColor = BRIGHT_RED;
                textColor = BRIGHT_RED;
                break;
            case FATAL:
                tag = "FATAL";
                tagColor = BRIGHT_RED;
                textColor = BRIGHT_RED;
                break;
            default:
        }
        tag = tag != null ? "[" + tag + "]" : "";
        tag = alignLeft(tag, 9);
        if (tagColor != null) {
            tag = tagColor + tag + ANSI_RESET;
        }
        timestamp = BRIGHT_BLACK + alignRight(timestamp, 10) + " - " + ANSI_RESET;
        if (textColor != null) {
            text = textColor + text + ANSI_RESET;
        }
        System.out.println(tag + timestamp + text);
    }

    public static String alignLeft(String string, int spaces) {
        String str = string;
        for (int i = string.length(); i < spaces; i++)
            str += " ";
        return str;
    }

    public static String alignRight(String string, int spaces) {
        String str = "";
        for (int i = string.length(); i < spaces; i++)
            str += " ";
        str += string;
        return str;
    }

    private static final List<String> followColors = Arrays.asList(BLUE, GREEN, PURPLE, CYAN, YELLOW, RED);
    private static final Map<Job, String> followColorByJob = new HashMap<>();
    private static int followBiggestNameSize = 0;
    private static String followProjectName;

    static void initializeFollow(Pipeline pipeline, DockerCompose dockerCompose) {
        int jobIndex = 0;
        for (Job job : pipeline.jobs) {
            int colorIndex = jobIndex % followColors.size();
            followColorByJob.put(job, followColors.get(colorIndex));
            for (int i = 1; i <= job.scale; i++) {
                String name = job.name + "_" + i;
                int len = name.length();
                if (len > followBiggestNameSize) {
                    followBiggestNameSize = len;
                }
            }
            jobIndex++;
        }
        followProjectName = dockerCompose.projectName;
    }

    static void follow(Job job, String containerName, String msg) {
        Logger.follow(() -> {
            String color = followColorByJob.get(job);
            String name;
            if (containerName == null) {
                name = job.name;
            } else {
                name = containerName.substring(followProjectName.length() + 1);
                if (job.scale == 1) {
                    name = name.substring(0, name.lastIndexOf("_"));
                }
            }
            name = Logger.alignLeft(name, followBiggestNameSize);
            return "" + color + name + " | " + Logger.ANSI_RESET + msg;
        });
    }
}