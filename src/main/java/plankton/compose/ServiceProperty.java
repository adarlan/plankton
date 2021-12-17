package plankton.compose;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ServiceProperty<T> {

    public final String key;

    protected Path resolvePathsFrom;

    public final Set<String> ignoredKeys = new HashSet<>();
    // public final Set<String> overriddenKeys = new HashSet<>();

    protected ServiceProperty(String key) {
        this.key = key;
    }

    public abstract void initialize(Object object);

    public abstract T applyTo(T other);

    protected String resolvePath(String pathString) {
        Path path = Paths.get(pathString);
        Path resolvedPath = resolvePathsFrom.resolve(path);
        try {
            return resolvedPath.toAbsolutePath().toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new ComposeFormatException("Unable to resolve path: " + pathString, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> castToMapOfObjects(Object object) {
        return (Map<String, Object>) object;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> castToMapOfStrings(Object object) {
        return (Map<String, String>) object;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> castToMapOfMaps(Object object) {
        return (Map<String, Map<String, Object>>) object;
    }

    @SuppressWarnings("unchecked")
    public static List<String> castToStringList(Object object) {
        return (List<String>) object;
    }

    public static List<String> convertToKeyValueList(Map<String, String> keyValueMap) {
        List<String> list = new ArrayList<>();
        keyValueMap.forEach((k, v) -> {
            if (v == null)
                list.add(k);
            else
                list.add(k + "=" + v);
        });
        return list;
    }
}
