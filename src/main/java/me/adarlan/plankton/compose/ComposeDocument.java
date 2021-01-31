package me.adarlan.plankton.compose;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.adarlan.plankton.util.YamlUtils;

@EqualsAndHashCode(of = { "filePath", "resolvePathsFrom" })
@Accessors(fluent = true)
public class ComposeDocument {

    // TODO ignore services based on profiles

    @Getter
    private final Path filePath;

    @Getter
    private final Path resolvePathsFrom;

    private Map<String, Object> documentMap;

    private final Set<String> serviceNames = new HashSet<>();
    private final Set<ComposeService> services = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ComposeDocument.class);
    private final String logPrefix;

    public ComposeDocument(ComposeDocumentConfiguration configuration) {

        this.filePath = configuration.filePath();
        this.resolvePathsFrom = configuration.resolvePathsFrom();
        this.logPrefix = resolvePathsFrom.relativize(filePath).toString() + " ... ";

        readFile();
        removeUnsupportedTopLevelKeys();
        initializeServices();
    }

    private void readFile() {
        documentMap = YamlUtils.loadFrom(filePath);
        logger.debug("{}Document map: {}", logPrefix, documentMap);
    }

    private void removeUnsupportedTopLevelKeys() {
        Set<String> keys = new HashSet<>(documentMap.keySet());
        keys.forEach(key -> {
            if (!key.equals(ComposeService.PARENT_KEY)) {
                logger.warn("{}Ignoring key: {}", logPrefix, key);
                documentMap.remove(key);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initializeServices() {
        logger.debug("{}Initializing services", logPrefix);
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
            logger.error("{}Invalid services: {}", logPrefix, invalidServices);
            throw new ComposeFileFormatException("Invalid services: " + invalidServices);
        }
        services.forEach(service -> {
            if (service.extends1 != null) {
                // TODO service.extendsFrom(other)
            }
        });
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
