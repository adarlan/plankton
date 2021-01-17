package me.adarlan.plankton.compose;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import me.adarlan.plankton.bash.BashScript;

public class ComposeDocument {

    @Getter
    private final String projectName;

    @Getter
    private final String filePath;

    @Getter
    private final String projectDirectory;

    @Getter
    private final String metadataDirectory;

    private Map<String, Object> documentMap;
    private Map<String, Object> servicesMap;
    private final Set<String> serviceNames = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ComposeDocument(ComposeDocumentConfiguration configuration) {
        this.projectName = configuration.projectName();
        this.projectDirectory = configuration.projectDirectory();
        this.metadataDirectory = configuration.metadataDirectory();
        this.filePath = metadataDirectory + "/compose.yaml";
        this.createMetadataDirectory();
        this.initializeFile(configuration.filePath());
        this.initializeDocumentMap();
    }

    private void createMetadataDirectory() {
        logger.info("Creating Compose metadata directory");
        BashScript script = new BashScript("create_metadata_directory");
        script.command("mkdir -p " + metadataDirectory);
        script.runSuccessfully();
    }

    private void initializeFile(String originalFile) {
        logger.info("Initializing Compose file");
        BashScript script = new BashScript("initialize_file");
        script.command("docker-compose --file " + originalFile + " config > " + filePath);
        script.command("cat " + filePath);
        script.runSuccessfully();
    }

    @SuppressWarnings("unchecked")
    private void initializeDocumentMap() {
        logger.info("Initializing Compose document map");
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            final Yaml yaml = new Yaml();
            documentMap = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new ComposeDocumentException("Unable to initialize Compose document map", e);
        }
        servicesMap = (Map<String, Object>) documentMap.get("services");
        servicesMap.keySet().forEach(serviceNames::add);
    }

    public Map<String, Object> getDocumentMap() {
        return Collections.unmodifiableMap(documentMap);
    }

    public Map<String, Object> getServicesMap() {
        return Collections.unmodifiableMap(servicesMap);
    }

    public Set<String> getServiceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServiceMap(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return Collections.unmodifiableMap(serviceMap);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getServiceLabelsMap(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return (Map<String, String>) serviceMap.computeIfAbsent("labels", k -> new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getServicePorts(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return (List<Map<String, Object>>) serviceMap.computeIfAbsent("ports", k -> new ArrayList<>());
        // TODO the 'published' key is optional
        // add this key where it is null
    }
}
