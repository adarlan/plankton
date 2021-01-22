package me.adarlan.plankton.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;
import me.adarlan.plankton.yaml.YamlUtils;

@ToString(of = { "projectName", "filePath", "projectDirectory" })
public class ComposeDocument {

    private final String projectName;
    private final String filePath;
    private final String projectDirectory;

    private Map<String, Object> documentMap;
    private Map<String, Object> servicesMap;
    private final Set<String> serviceNames = new HashSet<>();

    private final Set<ComposeService> services = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(ComposeDocument.class);
    private static final String LOADING = "Loading " + ComposeDocument.class.getSimpleName() + " ... ";

    public ComposeDocument(ComposeDocumentConfiguration configuration) {

        logger.info(LOADING);

        this.projectName = configuration.projectName();
        this.projectDirectory = configuration.projectDirectory();
        this.filePath = configuration.filePath();

        logger.info("{}projectName={}", LOADING, projectName);
        logger.info("{}projectDirectory={}", LOADING, projectDirectory);
        logger.info("{}filePath={}", LOADING, filePath);

        configureFile();

        readFile();
        logger.debug("{}documentMap={}", LOADING, documentMap);

        validateTopLevelKeys();
        initializeServicesMap();
        initializeServiceNames();
        initializeServices();
    }

    private void configureFile() {
        logger.info("{}Configuring file", LOADING);
        BashScript script = new BashScript();
        script.command("mv " + filePath + " " + filePath + ".original.yaml");
        script.command("docker-compose --file " + filePath + ".original.yaml --project-directory " + projectDirectory
                + " config > " + filePath);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new ComposeDocumentException("Unable to configure file", e);
        }
    }

    private void expandVariables() {
        // TODO expandVariables instead of configureFile
    }

    private void readFile() {
        logger.trace("{}Reading file", LOADING);
        documentMap = YamlUtils.loadFromFilePath(filePath);
    }

    private void validateTopLevelKeys() {
        Set<String> keys = new HashSet<>(documentMap.keySet());
        keys.forEach(key -> {
            if (!key.equals("services")) {
                logger.warn("{}Ignoring: {}", LOADING, key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initializeServicesMap() {
        this.servicesMap = (Map<String, Object>) documentMap.get("services");
        logger.debug("{}servicesMap={}", LOADING, servicesMap);
    }

    private void initializeServiceNames() {
        servicesMap.keySet().forEach(serviceNames::add);
        logger.debug("{}serviceNames={}", LOADING, serviceNames);
    }

    private void validateServiceNames() {
        // TODO validateServiceNames
    }

    private void initializeServices() {
        servicesMap.forEach((key, object) -> services.add(new ComposeService(key, object)));
    }

    public String projectName() {
        return projectName;
    }

    public String filePath() {
        return filePath;
    }

    public String projectDirectory() {
        return projectDirectory;
    }

    public Map<String, Object> documentMap() {
        return Collections.unmodifiableMap(documentMap);
    }

    public Map<String, Object> servicesMap() {
        return Collections.unmodifiableMap(servicesMap);
    }

    public Set<String> serviceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> serviceMapOf(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return Collections.unmodifiableMap(serviceMap);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> labelsMapOf(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return (Map<String, String>) serviceMap.computeIfAbsent("labels", k -> new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> servicePortsOf(String serviceName) {
        Map<String, Object> serviceMap = (Map<String, Object>) servicesMap.get(serviceName);
        return (List<Map<String, Object>>) serviceMap.computeIfAbsent("ports", k -> new ArrayList<>());
        // TODO the 'published' key is optional
        // add this key where it is null
    }
}
