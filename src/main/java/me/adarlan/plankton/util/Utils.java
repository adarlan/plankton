package me.adarlan.plankton.util;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {
        super();
    }

    public static String join(List<String> list, String delimiter) {
        return list.stream().collect(Collectors.joining(delimiter));
    }
}
