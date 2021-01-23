package me.adarlan.plankton.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;
import me.adarlan.plankton.util.YamlUtils;

@ToString(of = { "projectName", "filePath", "projectDirectory" })
public class ComposeDocument {

    // TODO secrets

    private final String projectName;
    private final String filePath;
    private final String projectDirectory;

    private Map<String, Object> map;

    private final Set<String> serviceNames = new HashSet<>();
    private final Set<ComposeService> services = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

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

        expandVariables();
        readFile();
        removeUnsupportedTopLevelKeys();
        initializeServices();
    }

    private void expandVariables() {
        // TODO expandVariables
        // inject variables: from configuration, from git...
        // mv compose.yaml compose.yaml.before_expansion.yaml
        // cat compose.yaml.before_expansion.yaml | envsubst > compose.yaml
    }

    private void readFile() {
        logger.trace("{}Reading file", LOADING);
        map = YamlUtils.loadFromFilePath(filePath);
        logger.debug("{}map={}", LOADING, map);
    }

    private void removeUnsupportedTopLevelKeys() {
        Set<String> keys = new HashSet<>(map.keySet());
        keys.forEach(key -> {
            if (!key.equals("services")) {
                logger.warn("{}Ignoring: {}", LOADING, key);
                map.remove(key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initializeServices() {
        Map<String, Object> servicesMap = (Map<String, Object>) map.get("services");
        servicesMap.forEach((serviceName, object) -> {
            ComposeService service = new ComposeService(serviceName, object);
            services.add(service);
            serviceNames.add(serviceName);
            servicesByName.put(serviceName, service);
        });
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

    public Set<ComposeService> services() {
        return Collections.unmodifiableSet(services);
    }

    public ComposeService serviceOfName(String serviceName) {
        return servicesByName.get(serviceName);
    }

    public String containerNameOf(ComposeService service, int containerIndex) {
        return projectName + "_" + service.name() + "_" + containerIndex;
    }

    public Set<String> serviceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }
}
