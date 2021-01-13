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

import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import me.adarlan.plankton.logging.Logger;
import me.adarlan.plankton.bash.BashScript;

public class Compose {

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

    private final Logger logger = Logger.getLogger();

    public Compose(ComposeConfiguration configuration) {
        this.projectName = configuration.projectName();
        this.projectDirectory = configuration.projectDirectory();
        this.metadataDirectory = configuration.metadataDirectory();
        this.filePath = metadataDirectory + "/compose.yaml";
        this.createMetadataDirectory();
        this.initializeFileFromOriginalFile(configuration.filePath());
        this.initializeDocumentMap();
    }

    private void createMetadataDirectory() {
        BashScript script = createScript("createMetadataDirectory");
        script.command("mkdir -p " + metadataDirectory);
        script.runSuccessfully();
    }

    private void initializeFileFromOriginalFile(String originalFile) {
        BashScript script = createScript("initializeFileFromOriginalFile");
        script.command("docker-compose --file " + originalFile + " config > " + filePath);
        script.command("cat " + filePath);
        script.runSuccessfully();
        // TODO does it require docker?
    }

    @SuppressWarnings("unchecked")
    private void initializeDocumentMap() {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            final Yaml yaml = new Yaml();
            documentMap = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new ComposeException("Unable to initialize document map", e);
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
        Map<String, Object> service = getServiceMap(serviceName);
        String key = "labels";
        if (service.containsKey(key)) {
            return (Map<String, String>) service.get(key);
        } else {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getServicePorts(String serviceName) {
        Map<String, Object> service = getServiceMap(serviceName);
        String key = "ports";
        if (service.containsKey(key)) {
            return (List<Map<String, Object>>) service.get(key);
        } else {
            return new ArrayList<>();
        }
        // TODO the 'published' key is optional
        // add this key where it is null
    }

    private BashScript createScript(String name) {
        BashScript script = new BashScript(name);
        script.forEachVariable(variable -> logger.debug(() -> script.getName() + " | " + variable));
        script.forEachCommand(command -> logger.debug(() -> script.getName() + " | " + command));
        script.forEachOutput(output -> logger.debug(() -> script.getName() + " >> " + output));
        script.forEachError(error -> logger.error(() -> script.getName() + " >> " + error));
        return script;
    }
}