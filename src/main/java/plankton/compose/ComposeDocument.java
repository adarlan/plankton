package plankton.compose;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import plankton.util.YamlUtils;

@EqualsAndHashCode(of = { "filePath", "resolvePathsFrom" })
@Accessors(fluent = true)
public class ComposeDocument {

    @Getter
    private final Path filePath;

    @Getter
    private final Path resolvePathsFrom;

    private Map<String, Object> documentMap;

    private final Set<String> serviceNames = new HashSet<>();
    private final Set<ComposeService> services = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

    private final Set<String> targetServiceNames;

    private static final Logger logger = LoggerFactory.getLogger(ComposeDocument.class);
    private final String logPrefix;

    public ComposeDocument(ComposeDocumentConfiguration configuration) {

        this.filePath = configuration.filePath();
        this.resolvePathsFrom = configuration.resolvePathsFrom();
        this.logPrefix = resolvePathsFrom.relativize(filePath).toString() + " ... ";

        logger.debug("{} ... filePath={}", getClass().getSimpleName(), filePath);
        logger.debug("{} ... resolvePathsFrom={}", getClass().getSimpleName(), resolvePathsFrom);

        readFile();
        removeUnsupportedTopLevelKeys();
        initializeServices();

        targetServiceNames = initializeTargetServiceNames(configuration);
        keepOnlyTargetServicesAndItsDependencies();
        logger.debug("{} ... targetServiceNames={}", getClass().getSimpleName(), targetServiceNames);
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
        servicesMap.forEach((serviceName, propertiesMap) -> {
            ComposeService service = new ComposeService(this, serviceName, propertiesMap);
            services.add(service);
            servicesByName.put(serviceName, service);
            if (!service.valid)
                invalidServices.add(service.name());
        });
        if (!invalidServices.isEmpty()) {
            logger.error("{}Invalid services: {}", logPrefix, invalidServices);
            throw new ComposeFormatException("Invalid services: " + invalidServices);
        }
        services.forEach(ComposeService::afterInitialization);
    }

    private Set<String> initializeTargetServiceNames(ComposeDocumentConfiguration configuration) {
        Set<String> target = configuration.targetServices();
        if (target.isEmpty())
            return new HashSet<>(serviceNames);
        else
            return new HashSet<>(target);
    }

    private final Set<ComposeService> activeServices = new HashSet<>();

    private void keepOnlyTargetServicesAndItsDependencies() {
        activeServicesAndItsDependencies();
        Set<ComposeService> toRemove = new HashSet<>();
        services.forEach(service -> {
            if (!activeServices.contains(service)) {
                toRemove.add(service);
            }
        });
        toRemove.forEach(service -> {
            services.remove(service);
            serviceNames.remove(service.name());
            servicesByName.remove(service.name(), service);
        });
    }

    private void activeServicesAndItsDependencies() {
        targetServiceNames.forEach(name -> {
            ComposeService s = serviceOfName(name);
            activeServiceAndItsDependencies(s);
        });
    }

    private void activeServiceAndItsDependencies(ComposeService service) {
        if (activeServices.contains(service))
            return;
        activeServices.add(service);
        Map<String, DependsOnCondition> dependsOn = service.dependsOn();
        logger.debug("service {} depends on: {}", service, dependsOn);
        dependsOn.forEach((name, condition) -> {
            ComposeService s = serviceOfName(name);
            activeServiceAndItsDependencies(s);
        });
    }

    private final Map<String, ComposeDocument> others = new HashMap<>();

    ComposeDocument getOther(String filePath) {
        // TODO if it is the same file path, return this
        if (others.containsKey(filePath))
            return others.get(filePath);
        ComposeDocument other = new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(filePath);
            }

            @Override
            public Path resolvePathsFrom() {
                return resolvePathsFrom;
            }

            @Override
            public Set<String> targetServices() {
                return new HashSet<>();
            }
        });
        others.forEach(other.others::put);
        others.put(filePath, other);
        return other;
        // TODO catch infinity loop
    }

    public Set<ComposeService> services() {
        return Collections.unmodifiableSet(services);
    }

    public ComposeService serviceOfName(String serviceName) {
        if (!servicesByName.containsKey(serviceName))
            throw new ComposeFormatException("Service not defined: " + serviceName);
        return servicesByName.get(serviceName);
    }

    public Set<String> serviceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }
}
