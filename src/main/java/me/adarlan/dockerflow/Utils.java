package me.adarlan.dockerflow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPropertyMap(final Map<String, Object> map, final String key) {
        Map<String, Object> m;
        Object o = null;
        if (map.containsKey(key)) {
            o = map.get(key);
        }
        if (o == null) {
            m = new HashMap<>();
            map.put(key, m);
        } else {
            m = (Map<String, Object>) o;
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPropertyStringList(final Map<String, Object> map, final String key) {
        List<String> list;
        Object o = null;
        if (map.containsKey(key)) {
            o = map.get(key);
        }
        if (o == null) {
            list = new ArrayList<>();
            map.put(key, list);
        } else {
            list = (List<String>) o;
        }
        return list;
    }

    public static void writeToYamlFile(final Map<String, Object> map, final String filePath) {
        try (Writer fileWriter = new FileWriter(filePath);) {
            final DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setPrettyFlow(true);
            final Yaml yml = new Yaml(dumperOptions);
            yml.dump(map, fileWriter);
        } catch (final IOException e) {
            throw new DockerflowException(e);
        }
    }

    public static Map<String, Object> createMapFromYamlFile(final String filePath) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            final Yaml yaml = new Yaml();
            return yaml.load(fileInputStream);
        } catch (final FileNotFoundException e) {
            throw new DockerflowException(e);
        } catch (final IOException e) {
            throw new DockerflowException(e);
        }
    }
}