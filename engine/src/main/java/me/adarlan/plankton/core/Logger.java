package me.adarlan.plankton.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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

    private static Level level = Level.LOG;
    private final Instant begin;

    private boolean printTimeStamp = false;

    private final Map<Service, String> serviceColorMap = new HashMap<>();
    private Integer biggestServiceNameLength = null;

    public enum Level {
        TRACE(0), DEBUG(1), LOG(2), INFO(3), WARN(4), ERROR(5), FATAL(6);

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

    public <T extends Service> void serviceInfo(T service) {
        if (level.accept(Level.INFO)) {
            // info(() -> prefix(service, null) + " " + status(service.getStatus()));
            info(() -> service.getName() + " -> " + service.getStatus());
        }
    }

    public <T extends ServiceDependency> void serviceDependencyInfo(T dependency) {
        if (level.accept(Level.INFO)) {
            // info(() -> prefix(dependency.getParentService(), null) + " " +
            // dependency.toString()
            // + status(dependency.getStatus()));
            info(dependency::toString);
        }
    }

    public void info(Supplier<String> supplier) {
        if (level.accept(Level.INFO))
            print(Level.INFO, supplier.get());
    }

    public void log(Service service, Supplier<String> supplier) {
        if (level.accept(Level.LOG))
            print(Level.LOG, prefix(service, null) + " " + supplier.get());
    }

    public void log(ServiceInstance serviceInstance, Supplier<String> supplier) {
        if (level.accept(Level.LOG))
            print(Level.LOG, prefix(serviceInstance.getParentService(), serviceInstance.getContainerName()) + " "
                    + supplier.get());
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
            case LOG:
                tag = "LOG";
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
        tag = alignLeft(tag, 8);
        if (tagColor != null) {
            tag = tagColor + tag + ANSI_RESET;
        }
        if (printTimeStamp) {
            timestamp = BRIGHT_BLACK + alignRight(timestamp, 10) + " - " + ANSI_RESET;
        } else {
            timestamp = "";
        }
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

    private String prefix(Service service, String containerName) {
        Pipeline pipeline = service.getPipeline();
        String name;
        if (containerName == null) {
            name = service.getName();
        } else {
            name = containerName.substring(pipeline.getId().length() + 1);
            if (service.getScale() == 1) {
                name = name.substring(0, name.lastIndexOf("_"));
            }
        }
        if (biggestServiceNameLength == null) {
            initializeBiggestServiceNameLength(pipeline);
        }
        String color = serviceColorMap.get(service);
        name = alignLeft(name, biggestServiceNameLength);
        return "" + color + name + Logger.ANSI_RESET + BRIGHT_BLACK + " | " + Logger.ANSI_RESET;
    }

    private void initializeBiggestServiceNameLength(Pipeline pipeline) {
        biggestServiceNameLength = 0;
        int serviceIndex = 0;
        for (Service service : pipeline.getServices()) {
            int colorIndex = serviceIndex % JOB_COLOR_LIST.size();
            serviceIndex++;
            String color = JOB_COLOR_LIST.get(colorIndex);
            debug(() -> "service: " + service.getName() + "; color index: " + colorIndex + "; color: " + color + "###"
                    + ANSI_RESET);
            serviceColorMap.put(service, color);
            for (int i = 1; i <= service.getScale(); i++) {
                String name = service.getName() + "_" + i;
                int len = name.length();
                if (len > biggestServiceNameLength) {
                    biggestServiceNameLength = len;
                }
            }
        }
    }

    // private String status(ServiceStatus status) {
    //     String color = "";
    //     switch (status) {
    //         case DISABLED:
    //             color = BRIGHT_PURPLE;
    //             break;
    //         case WAITING:
    //             color = BRIGHT_CYAN;
    //             break;
    //         case BLOCKED:
    //             color = BRIGHT_RED;
    //             break;
    //         case RUNNING:
    //             color = BRIGHT_BLUE;
    //             break;
    //         case FAILURE:
    //             color = BRIGHT_RED;
    //             break;
    //         case SUCCESS:
    //             color = BRIGHT_GREEN;
    //             break;
    //         default:
    //             break;
    //     }
    //     return color + status.toString() + ANSI_RESET;
    // }

    // private String status(ServiceDependencyStatus status) {
    //     String color = "";
    //     switch (status) {
    //         case WAITING:
    //             color = BRIGHT_CYAN;
    //             break;
    //         case PASSED:
    //             color = BRIGHT_GREEN;
    //             break;
    //         case BLOCKED:
    //             color = BRIGHT_RED;
    //             break;
    //     }
    //     return color + status.toString() + ANSI_RESET;
    // }
}