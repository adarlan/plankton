package me.adarlan.plankton.logging;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import me.adarlan.plankton.core.Pipeline;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceInstance;

public class Logger {

    private final Pipeline pipeline;
    private final Level level;
    private final Instant begin;

    private Integer biggestNameLength = null;
    private final Map<Service, String> serviceColorMap = new HashMap<>();

    private boolean printTimeStamp = false;

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

    public Logger(Pipeline pipeline, Level level) {
        this.pipeline = pipeline;
        this.level = level;
        this.begin = Instant.now();
        initializeBiggestNameLength();
        initializeServiceColors();
    }

    private void initializeBiggestNameLength() {
        biggestNameLength = 0;
        for (Service service : pipeline.getServices()) {
            for (int i = 1; i <= service.getScale(); i++) {
                String name = service.getName() + "_" + i;
                int len = name.length();
                if (len > biggestNameLength) {
                    biggestNameLength = len;
                }
            }
        }
    }

    private void initializeServiceColors() {
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_RED);
        list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_YELLOW);
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_PURPLE);
        list.add(Colors.BRIGHT_CYAN);
        int serviceIndex = 0;
        for (Service service : pipeline.getServices()) {
            int colorIndex = serviceIndex % list.size();
            String color;
            color = list.get(colorIndex);
            serviceIndex++;
            debug(() -> service.getName() + " is " + color + "###" + Colors.ANSI_RESET);
            serviceColorMap.put(service, color);
        }
    }

    public void trace(Supplier<String> supplier) {
        if (level.accept(Level.TRACE))
            print(Level.TRACE, colorizedText(Colors.BRIGHT_BLACK, supplier.get()));
    }

    public void debug(Supplier<String> supplier) {
        if (level.accept(Level.DEBUG))
            print(Level.DEBUG, colorizedText(Colors.BRIGHT_BLACK, supplier.get()));
    }

    public void log(Service service, Supplier<String> supplier) {
        if (level.accept(Level.LOG))
            print(Level.LOG, logPrefixOf(service) + supplier.get());
    }

    public void log(ServiceInstance instance, Supplier<String> supplier) {
        if (level.accept(Level.LOG))
            print(Level.LOG, logPrefixOf(instance) + supplier.get());
    }

    public void info(Supplier<String> supplier) {
        if (level.accept(Level.INFO))
            print(Level.INFO, colorizedText(Colors.BRIGHT_WHITE, supplier.get()));
    }

    public void info(Service service, Supplier<String> supplier) {
        if (level.accept(Level.INFO))
            print(Level.INFO, infoPrefixOf(service) + colorizedText(Colors.BRIGHT_WHITE, supplier.get()));
    }

    public void warn(Supplier<String> supplier) {
        if (level.accept(Level.WARN))
            print(Level.WARN, colorizedText(Colors.YELLOW, supplier.get()));
    }

    public void error(Supplier<String> supplier) {
        if (level.accept(Level.ERROR))
            print(Level.ERROR, colorizedText(Colors.RED, supplier.get()));
    }

    public void fatal(Supplier<String> supplier) {
        if (level.accept(Level.FATAL))
            print(Level.FATAL, colorizedText(Colors.RED, supplier.get()));
    }

    private void print(Level level, String text) {
        Long time = Duration.between(begin, Instant.now()).toMillis();
        String tagColor = null;
        switch (level) {
            case TRACE:
                tagColor = Colors.BRIGHT_BLACK;
                break;
            case DEBUG:
                tagColor = Colors.BRIGHT_BLACK;
                break;
            case LOG:
                tagColor = Colors.WHITE;
                break;
            case INFO:
                tagColor = Colors.WHITE;
                break;
            case WARN:
                tagColor = Colors.YELLOW;
                break;
            case ERROR:
                tagColor = Colors.RED;
                break;
            case FATAL:
                tagColor = Colors.RED;
                break;
        }
        String tag;
        tag = level.toString();
        tag = "[" + tag + "]";
        tag = alignLeft(tag, 8);
        tag = colorizedText(tagColor, tag);
        if (printTimeStamp) {
            String timestamp = colorizedText(Colors.BRIGHT_BLACK, alignRight(time.toString(), 10) + " - ");
            System.out.println(tag + timestamp + text);
        } else {
            System.out.println(tag + text);
        }
    }

    private String alignLeft(String color, String string, int spaces, String startWith, String fillWith,
            String endWith) {
        StringBuilder sb = new StringBuilder();
        sb.append(color);
        sb.append(string);
        sb.append(Colors.ANSI_RESET);
        sb.append(Colors.BRIGHT_BLACK);
        sb.append(startWith);
        for (int i = string.length(); i < spaces; i++)
            sb.append(fillWith);
        sb.append(endWith);
        sb.append(Colors.ANSI_RESET);
        return sb.toString();
    }

    private String alignLeft(String string, int spaces) {
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        for (int i = string.length(); i < spaces; i++)
            sb.append(" ");
        return sb.toString();
    }

    private String alignRight(String string, int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = string.length(); i < spaces; i++)
            sb.append(" ");
        sb.append(string);
        return sb.toString();
    }

    private String infoPrefixOf(Service service) {
        return prefix(service, null, "-", "-> ");
    }

    private String logPrefixOf(Service service) {
        return prefix(service, null, " ", "    |");
    }

    private String logPrefixOf(ServiceInstance instance) {
        Service service = instance.getParentService();
        String containerName = instance.getContainerName();
        return prefix(service, containerName, " ", "    |");
    }

    private String prefix(Service service, String containerName, String fillWith, String endWith) {
        String name;
        if (containerName == null) {
            name = service.getName();
        } else {
            name = containerName.substring(pipeline.getId().length() + 1);
            if (service.getScale() == 1) {
                name = name.substring(0, name.lastIndexOf("_"));
            }
        }
        String color = serviceColorMap.get(service);
        return alignLeft(color, name, biggestNameLength, " ", fillWith, endWith);
    }

    private String colorizedText(String color, String text) {
        return color + text + Colors.ANSI_RESET;
    }
}