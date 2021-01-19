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
import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;

@ToString(of = { "projectName", "filePath", "projectDirectory" })
public class ComposeDocument {

    @Getter
    private final String projectName;

    @Getter
    private final String filePath;

    @Getter
    private final String projectDirectory;

    private Map<String, Object> documentMap;
    private Map<String, Object> servicesMap;
    private final Set<String> serviceNames = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ComposeDocument(ComposeDocumentConfiguration configuration) {
        logger.info("Loading ComposeDocument");
        this.projectName = configuration.projectName();
        logger.info("projectName={}", projectName);
        this.projectDirectory = configuration.projectDirectory();
        logger.info("projectDirectory={}", projectDirectory);
        this.filePath = configuration.filePath();
        logger.info("filePath={}", filePath);
        configureFile();
        initializeDocumentMap();
    }

    private void configureFile() {
        logger.info("Configuring file");
        BashScript script = new BashScript("initialize_file");
        script.command("mv " + filePath + " " + filePath + ".original.yaml");
        script.command("docker-compose --file " + filePath + ".original.yaml config > " + filePath);
        script.command("cat " + filePath);
        script.runSuccessfully();
    }

    @SuppressWarnings("unchecked")
    private void initializeDocumentMap() {
        logger.info("Initializing document map");
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            final Yaml yaml = new Yaml();
            documentMap = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new ComposeDocumentException("Unable to initialize Compose document map", e);
        }
        this.servicesMap = (Map<String, Object>) documentMap.get("services");
        logger.info("servicesMap={}", servicesMap);
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
