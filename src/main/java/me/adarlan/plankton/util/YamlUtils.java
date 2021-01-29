package me.adarlan.plankton.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class YamlUtils {

    private YamlUtils() {
        super();
    }

    public static Map<String, Object> loadFromInputStream(InputStream inputStream) {
        final Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }

    public static Map<String, Object> loadFromFilePath(String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            return loadFromInputStream(fileInputStream);
        } catch (IOException e) {
            throw new YamlException("Unable to load from file path: " + filePath, e);
        }
    }
}
