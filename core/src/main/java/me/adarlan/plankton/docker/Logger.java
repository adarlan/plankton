package me.adarlan.plankton.docker;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import me.adarlan.plankton.api.Job;
import me.adarlan.plankton.api.JobStatus;
import me.adarlan.plankton.api.Pipeline;

public class Logger {

    private static final String ANSI_RESET = "\u001B[0m";

    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    private static final String BRIGHT_BLACK = "\u001b[30;1m";
    private static final String BRIGHT_RED = "\u001b[31;1m";
    private static final String BRIGHT_GREEN = "\u001b[32;1m";
    private static final String BRIGHT_YELLOW = "\u001b[33;1m";
    private static final String BRIGHT_BLUE = "\u001b[34;1m";
    private static final String BRIGHT_PURPLE = "\u001b[35;1m";
    private static final String BRIGHT_CYAN = "\u001b[36;1m";
    private static final String BRIGHT_WHITE = "\u001b[37;1m";

    private static final List<String> JOB_COLOR_LIST = Arrays.asList(BLUE, GREEN, PURPLE, CYAN, YELLOW, RED);

    private static Level level = Level.FOLLOW;
    private final Instant begin;

    private final Map<Job, String> jobColorMap = new HashMap<>();
    private Integer biggestJobNameLength = null;

    public enum Level {
        TRACE(0), DEBUG(1), FOLLOW(2), INFO(3), WARN(4), ERROR(5), FATAL(6);

        private final int value;

        private Level(int value) {
            this.value = value;
        }

        private boolean accept(Level other) {
            return value <= other.value;
        }
    }

    private static Logger logger = null;

    public static void setLevel(Level level) {
        Logger.level = level;
    }

    public static Logger getLogger() {
        if (logger == null) {
            logger = new Logger();
        }
        return logger;
    }

    private Logger() {
        this.begin = Instant.now();
    }

    void info(Job job) {
        if (level.accept(Level.INFO))
            info(() -> prefix(job, null) + " " + status(job.getStatus()));
    }

    void info(Rule rule) {
        if (level.accept(Level.INFO))
            info(() -> prefix(rule.getParentJob(), null) + " " + rule.getName() + "(" + rule.getValue() + ") "
                    + status(rule.getStatus()));
    }

    public void info(Supplier<String> supplier) {
        if (level.accept(Level.INFO))
            print(Level.INFO, supplier.get());
    }

    void log(me.adarlan.plankton.docker.Job job, String msg) {
        synchronized (job.logs) {
            job.logs.add(msg);
        }
        if (level.accept(Level.FOLLOW))
            follow(() -> prefix(job, null) + " " + msg);
    }

    void log(JobInstance jobInstance, String msg) {
        synchronized (jobInstance.logs) {
            jobInstance.logs.add(msg);
        }
        if (level.accept(Level.FOLLOW))
            follow(() -> prefix(jobInstance.getParentJob(), jobInstance.getContainerName()) + " " + msg);
    }

    private void follow(Supplier<String> supplier) {
        if (level.accept(Level.FOLLOW))
            print(Level.FOLLOW, supplier.get());
    }

    public void debug(Supplier<String> supplier) {
        if (level.accept(Level.DEBUG))
            print(Level.DEBUG, supplier.get());
    }

    public void error(Supplier<String> supplier) {
        if (level.accept(Level.ERROR))
            print(Level.ERROR, supplier.get());
    }

    public void trace(Supplier<String> supplier) {
        if (level.accept(Level.TRACE))
            print(Level.TRACE, supplier.get());
    }

    public void fatal(Supplier<String> supplier) {
        if (level.accept(Level.FATAL))
            print(Level.FATAL, supplier.get());
    }

    private void print(Level level, String text) {
        Long time = Duration.between(begin, Instant.now()).toMillis();
        String tag = null;
        String tagColor = null;
        String timestamp = time.toString();
        String textColor = null;
        switch (level) {
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

    private String alignLeft(String string, int spaces) {
        String str = string;
        for (int i = string.length(); i < spaces; i++)
            str += " ";
        return str;
    }

    private String alignRight(String string, int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = string.length(); i < spaces; i++)
            sb.append(" ");
        sb.append(string);
        return sb.toString();
    }

    private String prefix(Job job, String containerName) {
        Pipeline pipeline = job.getPipeline();
        String name;
        if (containerName == null) {
            name = job.getName();
        } else {
            name = containerName.substring(pipeline.getName().length() + 1);
            if (job.getScale() == 1) {
                name = name.substring(0, name.lastIndexOf("_"));
            }
        }
        if (biggestJobNameLength == null) {
            initializeBiggestJobNameLength(pipeline);
        }
        String color = jobColorMap.get(job);
        name = alignLeft(name, biggestJobNameLength);
        return "" + color + name + Logger.ANSI_RESET + BRIGHT_BLACK + " | " + Logger.ANSI_RESET;
    }

    private void initializeBiggestJobNameLength(Pipeline pipeline) {
        biggestJobNameLength = 0;
        int jobIndex = 0;
        for (Job job : pipeline.getJobs()) {
            int colorIndex = jobIndex % JOB_COLOR_LIST.size();
            jobIndex++;
            String color = JOB_COLOR_LIST.get(colorIndex);
            logger.debug(() -> "job: " + job.getName() + "; color index: " + colorIndex + "; color: " + color + "###"
                    + ANSI_RESET);
            jobColorMap.put(job, color);
            for (int i = 1; i <= job.getScale(); i++) {
                String name = job.getName() + "_" + i;
                int len = name.length();
                if (len > biggestJobNameLength) {
                    biggestJobNameLength = len;
                }
            }
        }
    }

    private String status(JobStatus status) {
        String color = "";
        switch (status) {
            case DISABLED:
                color = BRIGHT_PURPLE;
                break;
            case WAITING:
                color = BRIGHT_CYAN;
                break;
            case BLOCKED:
                color = BRIGHT_RED;
                break;
            case RUNNING:
                color = BRIGHT_BLUE;
                break;
            case FAILURE:
                color = BRIGHT_RED;
                break;
            case SUCCESS:
                color = BRIGHT_GREEN;
                break;
            default:
                break;
        }
        return color + status.toString() + ANSI_RESET;
    }

    private String status(RuleStatus status) {
        String color = "";
        switch (status) {
            case WAITING:
                color = BRIGHT_CYAN;
                break;
            case PASSED:
                color = BRIGHT_GREEN;
                break;
            case BLOCKED:
                color = BRIGHT_RED;
                break;
        }
        return color + status.toString() + ANSI_RESET;
    }
}