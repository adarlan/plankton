package me.adarlan.plankton.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import me.adarlan.plankton.util.FileSystemUtils;
import me.adarlan.plankton.util.YamlUtils;

@EqualsAndHashCode(of = "filePath")
public class ComposeDocument {

    // TODO secrets
    // TODO ignore services based on profiles

    private final String projectDirectory;
    private final String filePath;
    private final String relativeFilePath;

    private Map<String, Object> documentMap;

    private final Set<String> serviceNames = new HashSet<>();
    private final Set<ComposeService> services = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ComposeDocument.class);

    public ComposeDocument(ComposeDocumentConfiguration configuration) {

        this.projectDirectory = configuration.projectDirectory();
        this.filePath = configuration.filePath();
        this.relativeFilePath = FileSystemUtils.relativePath(projectDirectory, filePath);

        logger.debug("{} ... Loading", this);

        expandVariables();
        readFile();
        removeUnsupportedTopLevelKeys();
        initializeServices();
    }

    @Override
    public String toString() {
        return relativeFilePath;
    }

    private void expandVariables() {

        // TODO expandVariables

        // inject variables: from configuration, from git...
        // script.env(variables);

        // escape $
        // 1: replace all '$$' by '<$$>'
        // 2: envsubst
        // 3: replace all '<$$>' by '$'
        // is it right?
        // read envsubst documentation

        // put the result in a second file:
        // -> cat file1 | envsubst > file2

        // or get the input stream and give it to the yaml parser:
        // -> sed "s/$$/<$$>/g" | envsubst | sed "s/<$$>/$/g"
        // this way there is no need to change the workspace
    }

    private void readFile() {
        logger.debug("{} ... Reading file", this);
        documentMap = YamlUtils.loadFromFilePath(filePath);
        logger.debug("{} ... Reading file -> {}", this, documentMap);
    }

    private void removeUnsupportedTopLevelKeys() {
        Set<String> keys = new HashSet<>(documentMap.keySet());
        keys.forEach(key -> {
            if (!key.equals(ComposeService.PARENT_KEY)) {
                logger.warn("{} ... Ignoring key: {}", this, key);
                documentMap.remove(key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initializeServices() {
        logger.debug("{} ... Initializing services", this);
        Map<String, Map<String, Object>> servicesMap;
        servicesMap = (Map<String, Map<String, Object>>) documentMap.get(ComposeService.PARENT_KEY);
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
            logger.error("{} ... Invalid services: {}", this, invalidServices);
            throw new ComposeFileFormatException("Invalid services: " + invalidServices);
        }
        services.forEach(service -> {
            if (service.extends1 != null) {
                // TODO service.extendsFrom(other)
            }
        });
    }

    public String filePath() {
        return filePath;
    }

    public Set<ComposeService> services() {
        return Collections.unmodifiableSet(services);
    }

    public ComposeService serviceOfName(String serviceName) {
        return servicesByName.get(serviceName);
    }

    public Set<String> serviceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }
}
