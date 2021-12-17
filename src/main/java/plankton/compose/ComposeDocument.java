package plankton.compose;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = { "key" })
public class ComposeDocument {

    String key;
    Path filePath;
    Path resolvePathsFrom;

    Map<String, Object> documentMap;
    Map<String, Map<String, Object>> servicesMap;

    private final Set<ComposeService> services = new HashSet<>();
    private final Set<String> serviceNames = new HashSet<>();
    private final Map<String, ComposeService> servicesByName = new HashMap<>();

    ComposeDocument() {

    }

    void addService(ComposeService service) {
        services.add(service);
        serviceNames.add(service.name);
        servicesByName.put(service.name, service);
        servicesMap.computeIfAbsent(service.name, x -> new HashMap<>());
    }

    public Set<ComposeService> services() {
        return Collections.unmodifiableSet(services);
    }

    public ComposeService getServiceByName(String serviceName) {
        if (!servicesByName.containsKey(serviceName))
            throw new UndefinedServiceException(
                    "Service not defined: " + key + "::" + ComposeService.PARENT_KEY + "." + serviceName);
        return servicesByName.get(serviceName);
    }

    public Set<String> serviceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }
}
