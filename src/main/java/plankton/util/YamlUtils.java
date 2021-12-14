package plankton.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlUtils {

    private YamlUtils() {
        super();
    }

    public static Map<String, Object> loadFrom(String filePathString) {
        Path filePath = Paths.get(filePathString);
        return loadFrom(filePath);
    }

    public static Map<String, Object> loadFrom(Path filePath) {
        File file = filePath.toFile();
        return loadFrom(file);
    }

    public static Map<String, Object> loadFrom(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);) {
            return loadFrom(fileInputStream);
        } catch (IOException e) {
            throw new YamlUtilsException("Unable to load from file: " + file, e);
        }
    }

    public static Map<String, Object> loadFrom(InputStream inputStream) {
        final Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }
}
