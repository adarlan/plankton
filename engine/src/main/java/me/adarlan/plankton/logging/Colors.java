package me.adarlan.plankton.logging;

public class Colors {

    private Colors() {
    }

    public static final String ANSI_RESET = "\u001B[0m";

    public static final String BLACK = "\u001B[30m";
    public static final String WHITE = "\u001B[37m";

    public static final String BRIGHT_BLACK = "\u001b[30;1m";
    public static final String BRIGHT_WHITE = "\u001b[37;1m";

    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";

    public static final String BRIGHT_RED = "\u001b[31;1m";
    public static final String BRIGHT_GREEN = "\u001b[32;1m";
    public static final String BRIGHT_YELLOW = "\u001b[33;1m";
    public static final String BRIGHT_BLUE = "\u001b[34;1m";
    public static final String BRIGHT_PURPLE = "\u001b[35;1m";
    public static final String BRIGHT_CYAN = "\u001b[36;1m";
}
