package plankton.compose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

class YamlUtils {

    private YamlUtils() {
        super();
    }

    static Map<String, Object> loadFrom(String filePathString) throws IOException {
        Path filePath = Paths.get(filePathString);
        return loadFrom(filePath);
    }

    static Map<String, Object> loadFrom(Path filePath) throws IOException {
        File file = filePath.toFile();
        return loadFrom(file);
    }

    static Map<String, Object> loadFrom(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file);) {
            return loadFrom(fileInputStream);
        }
    }

    static Map<String, Object> loadFrom(InputStream inputStream) {
        final Yaml yaml = new Yaml();
        return yaml.load(inputStream);
    }
}
