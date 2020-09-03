package me.adarlan.dockerflow;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class Logger {
    private Logger() {
        super();
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

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

    public static void follow(Supplier<String> supplier) {
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

    private static void print(int LEVEL, String string) {
        Long time = Duration.between(begin, Instant.now()).toMillis();
        String color = null;
        String string1;
        String string2 = time.toString();
        switch (LEVEL) {
            case TRACE:
                string1 = "[TRACE]";
                break;
            case DEBUG:
                string1 = "[DEBUG]";
                break;
            case FOLLOW:
                string1 = "";
                break;
            case INFO:
                color = ANSI_CYAN;
                string1 = "[INFO]";
                break;
            case WARN:
                color = ANSI_YELLOW;
                string1 = "[WARN]";
                break;
            case ERROR:
                color = ANSI_RED;
                string1 = "[ERROR]";
                break;
            case FATAL:
                color = ANSI_RED;
                string1 = "[FATAL]";
                break;
            default:
                string1 = "";
        }
        string1 = alignLeft(string1, 8);
        if (color != null) {
            string1 = color + string1 + ANSI_RESET;
        }
        string2 = alignRight(string2, 10);
        System.out.println(string1 + string2 + " - " + string);
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
}