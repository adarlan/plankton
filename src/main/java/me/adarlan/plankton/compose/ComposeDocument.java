package me.adarlan.plankton.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.util.YamlUtils;

@EqualsAndHashCode(of = "projectName")
@ToString(of = { "projectName", "filePath", "projectDirectory" })
public class ComposeDocument {

    // TODO secrets
    // TODO ignore services based on profiles

    private final String projectName;
    private final String filePath;
    private final String projectDirectory;

    private Map<String, Object> map;

    private final Set<String> serviceNames = new HashSet<>();
    private final Set<ComposeService> services = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ComposeDocument.class);

    public ComposeDocument(ComposeDocumentConfiguration configuration) {

        this.projectName = configuration.projectName();
        this.projectDirectory = configuration.projectDirectory();
        this.filePath = configuration.filePath();

        logger.info("Loading {} ...", this);

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
        logger.info("Loading {} ... Reading file", this);
        map = YamlUtils.loadFromFilePath(filePath);
        logger.debug("Loading {} ... Reading file -> map={}", this, map);
    }

    private void removeUnsupportedTopLevelKeys() {
        Set<String> keys = new HashSet<>(map.keySet());
        keys.forEach(key -> {
            if (!key.equals("services")) {
                logger.warn("Loading {} ... Ignoring key: {}", this, key);
                map.remove(key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initializeServices() {
        logger.info("Loading {} ... Initializing services", this);
        Map<String, Map<String, Object>> servicesMap;
        servicesMap = (Map<String, Map<String, Object>>) map.get("services");
        servicesMap.keySet().forEach(serviceNames::add);
        Set<String> invalidServices = new HashSet<>();
        servicesMap.forEach((serviceName, object) -> {
            ComposeService service = new ComposeService(this, serviceName, object);
            services.add(service);
            servicesByName.put(serviceName, service);
            if (!service.valid)
                invalidServices.add(service.name());
        });
        if (!invalidServices.isEmpty()) {
            logger.error("Loading {} ... Invalid services: {}", this, invalidServices);
            throw new ComposeFileFormatException("Invalid services: " + invalidServices);
        }
        services.forEach(service -> {
            if (service.extends1 != null) {
                // TODO service.extendsFrom(other)
            }
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
