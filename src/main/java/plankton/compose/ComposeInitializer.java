package plankton.compose;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.compose.serviceprops.Build;
import plankton.compose.serviceprops.Command;
import plankton.compose.serviceprops.DependsOn;
import plankton.compose.serviceprops.Entrypoint;
import plankton.compose.serviceprops.EnvFile;
import plankton.compose.serviceprops.Environment;
import plankton.compose.serviceprops.Expose;
import plankton.compose.serviceprops.Extends;
import plankton.compose.serviceprops.GroupAdd;
import plankton.compose.serviceprops.Healthcheck;
import plankton.compose.serviceprops.Image;
import plankton.compose.serviceprops.Labels;
import plankton.compose.serviceprops.Profiles;
import plankton.compose.serviceprops.User;
import plankton.compose.serviceprops.Volumes;
import plankton.compose.serviceprops.WorkingDir;
import plankton.util.YamlUtils;

public class ComposeInitializer {

    private final ComposeDocumentConfiguration configuration;
    private final ComposeDocument mainComposeDocument;
    private final Map<Path, ComposeDocument> composeDocumentsByFilePath = new HashMap<>();

    private final Set<ComposeService> servicesWithExtendsResolved = new HashSet<>();
    private final Set<ComposeService> servicesWithDependsOnResolved = new HashSet<>();

    static final Logger logger = LoggerFactory.getLogger(ComposeInitializer.class);

    public ComposeInitializer(ComposeDocumentConfiguration config) {
        configuration = config;
        mainComposeDocument = composeDocumentByFilePath(configuration.filePath());
        resolveServicesExtends();
        resolveServicesDependsOn();
        mainComposeDocument.services().forEach(service -> {
            if (service.build != null)
                logger.debug("{} build = {}", service, service.build);
            if (service.command != null)
                logger.debug("{} command = {}", service, service.command);
            if (service.dependsOn != null)
                logger.debug("{} dependsOn = {}", service, service.dependsOn);
            if (service.entrypoint != null)
                logger.debug("{} entrypoint = {}", service, service.entrypoint);
            if (service.envFile != null)
                logger.debug("{} envFile = {}", service, service.envFile);
            if (service.environment != null)
                logger.debug("{} environment = {}", service, service.environment);
            if (service.expose != null)
                logger.debug("{} expose = {}", service, service.expose);
            if (service.extends1 != null)
                logger.debug("{} extends1 = {}", service, service.extends1);
            if (service.groupAdd != null)
                logger.debug("{} groupAdd = {}", service, service.groupAdd);
            if (service.healthcheck != null)
                logger.debug("{} healthcheck = {}", service, service.healthcheck);
            if (service.image != null)
                logger.debug("{} image = {}", service, service.image);
            if (service.labels != null)
                logger.debug("{} labels = {}", service, service.labels);
            if (service.profiles != null)
                logger.debug("{} profiles = {}", service, service.profiles);
            if (service.user != null)
                logger.debug("{} user = {}", service, service.user);
            if (service.volumes != null)
                logger.debug("{} volumes = {}", service, service.volumes);
            if (service.workingDir != null)
                logger.debug("{} workingDir = {}", service, service.workingDir);
        });
    }

    public ComposeDocument composeDocument() {
        return mainComposeDocument;
    }

    private ComposeDocument composeDocumentByFilePath(Path filePath) {
        if (composeDocumentsByFilePath.containsKey(filePath))
            return composeDocumentsByFilePath.get(filePath);
        ComposeDocument doc = new ComposeDocument();
        doc.filePath = filePath;
        doc.key = configuration.resolvePathsFrom().relativize(doc.filePath).toString();
        doc.resolvePathsFrom = configuration.resolvePathsFrom();
        initializeDocumentMap(doc);
        initializeServicesMap(doc);
        doc.documentMap.keySet()
                .forEach(ignoredTopLevelKey -> logger.warn("Ignoring {}::{}", doc.key, ignoredTopLevelKey));
        instantiateServices(doc);
        initializeServicesProperties(doc);
        doc.servicesMap.forEach((serviceName, servicePropertiesMap) -> servicePropertiesMap.keySet()
                .forEach(ignoredServicePropertyKey -> logger
                        .warn("Ignoring {}::{}.{}", doc.key, serviceName, ignoredServicePropertyKey)));
        composeDocumentsByFilePath.put(filePath, doc);
        return doc;
    }

    private void initializeDocumentMap(ComposeDocument composeDocument) {
        composeDocument.documentMap = YamlUtils.loadFrom(composeDocument.filePath);
    }

    @SuppressWarnings("unchecked")
    private void initializeServicesMap(ComposeDocument composeDocument) {
        composeDocument.servicesMap = (Map<String, Map<String, Object>>) composeDocument.documentMap
                .remove(ComposeService.PARENT_KEY);
    }

    private void instantiateServices(ComposeDocument composeDocument) {
        composeDocument.servicesMap.keySet().forEach(serviceName -> {
            ComposeService service = new ComposeService();
            service.composeDocument = composeDocument;
            service.name = serviceName;
            service.key = composeDocument.key + "::" + ComposeService.PARENT_KEY + "." + serviceName;
            composeDocument.addService(service);
        });
    }

    private void initializeServicesProperties(ComposeDocument doc) {
        doc.services().forEach(s -> s.build = initializeProperty(doc, s, Build::new));
        doc.services().forEach(s -> s.command = initializeProperty(doc, s, Command::new));
        doc.services().forEach(s -> s.dependsOn = initializeProperty(doc, s, DependsOn::new));
        doc.services().forEach(s -> s.entrypoint = initializeProperty(doc, s, Entrypoint::new));
        doc.services().forEach(s -> s.envFile = initializeProperty(doc, s, EnvFile::new));
        doc.services().forEach(s -> s.environment = initializeProperty(doc, s, Environment::new));
        doc.services().forEach(s -> s.expose = initializeProperty(doc, s, Expose::new));
        doc.services().forEach(s -> s.extends1 = initializeProperty(doc, s, Extends::new));
        doc.services().forEach(s -> s.groupAdd = initializeProperty(doc, s, GroupAdd::new));
        doc.services().forEach(s -> s.healthcheck = initializeProperty(doc, s, Healthcheck::new));
        doc.services().forEach(s -> s.image = initializeProperty(doc, s, Image::new));
        doc.services().forEach(s -> s.labels = initializeProperty(doc, s, Labels::new));
        doc.services().forEach(s -> s.profiles = initializeProperty(doc, s, Profiles::new));
        doc.services().forEach(s -> s.user = initializeProperty(doc, s, User::new));
        doc.services().forEach(s -> s.volumes = initializeProperty(doc, s, Volumes::new));
        doc.services().forEach(s -> s.workingDir = initializeProperty(doc, s, WorkingDir::new));
    }

    private <T extends ServiceProperty<T>> T initializeProperty(
            ComposeDocument composeDocument,
            ComposeService service,
            Supplier<T> constructor) {
        T property = constructor.get();
        logger.debug("service.name = {}", service.name);
        Map<String, Object> servicePropertiesMap = composeDocument.servicesMap.get(service.name);
        logger.debug("servicePropertiesMap = {}", servicePropertiesMap);
        if (servicePropertiesMap.containsKey(property.key)) {
            property.resolvePathsFrom = composeDocument.resolvePathsFrom;
            property.initialize(servicePropertiesMap.remove(property.key));
            property.ignoredKeys
                    .forEach(ignoredKey -> logger.warn("Ignoring {}::{}.{}.{}",
                            composeDocument.key, service.name, property.key, ignoredKey));
            service.properties.add(property);
            return property;
        } else {
            return null;
        }
    }

    private void resolveServicesExtends() {
        mainComposeDocument.services().forEach(this::resolveServiceExtends);
        mainComposeDocument.services().forEach(
                service -> service.parentServices.forEach(parentService -> parentService.childServices.add(service)));
    }

    private void resolveServiceExtends(ComposeService service) {
        if (servicesWithExtendsResolved.contains(service))
            return;
        servicesWithExtendsResolved.add(service);
        if (service.extends1 == null)
            return;
        ComposeDocument baseComposeDocument = (service.extends1.getFile() == null)
                ? mainComposeDocument
                : composeDocumentByFilePath(Paths.get(service.extends1.getFile()));
        String baseServiceName = service.extends1.getService();
        ComposeService parentService = baseComposeDocument.getServiceByName(baseServiceName);
        resolveServiceExtends(parentService);
        service.parentServices.add(parentService);
        service.parentServices.addAll(parentService.parentServices);
        if (service.parentServices.contains(service)) {
            throw new CircularExtendsException(
                    service.parentServices.stream().map(ComposeService::toString).collect(Collectors.joining(" --> ")));
        }
        resolveServiceExtends(parentService, service);
    }

    private void resolveServiceExtends(ComposeService parent, ComposeService service) {
        if (parent.dependsOn != null && parent.composeDocument != mainComposeDocument) {
            throw new UnreachableDependsOnException(
                    service.key + " extends " + parent.key
                            + ", which is from another file and depends on unreachable services");
        }
        logger.info("{} extends {}", service, parent);
        service.build = extendProperty(parent.build, service.build);
        service.command = extendProperty(parent.command, service.command);
        service.dependsOn = extendProperty(parent.dependsOn, service.dependsOn);
        service.entrypoint = extendProperty(parent.entrypoint, service.entrypoint);
        service.envFile = extendProperty(parent.envFile, service.envFile);
        service.environment = extendProperty(parent.environment, service.environment);
        service.expose = extendProperty(parent.expose, service.expose);
        service.groupAdd = extendProperty(parent.groupAdd, service.groupAdd);
        service.healthcheck = extendProperty(parent.healthcheck, service.healthcheck);
        service.image = extendProperty(parent.image, service.image);
        service.labels = extendProperty(parent.labels, service.labels);
        service.profiles = extendProperty(parent.profiles, service.profiles);
        service.user = extendProperty(parent.user, service.user);
        service.volumes = extendProperty(parent.volumes, service.volumes);
        service.workingDir = extendProperty(parent.workingDir, service.workingDir);
    }

    private <T extends ServiceProperty<T>> T extendProperty(
            T parentProperty,
            T childProperty) {
        if (parentProperty != null)
            childProperty = parentProperty.applyTo(childProperty);
        if (childProperty != null)
            logger.info("  {} = {}", childProperty.key, childProperty);
        return childProperty;
    }

    private void resolveServicesDependsOn() {
        mainComposeDocument.services().forEach(this::resolveServiceDependsOn);
    }

    private void resolveServiceDependsOn(ComposeService service) {
        if (servicesWithDependsOnResolved.contains(service))
            return;
        servicesWithDependsOnResolved.add(service);
        if (service.dependsOn == null)
            return;
        service.dependsOn.serviceConditionMap().forEach((dependencyName, dependencyCondition) -> {
            ComposeService dependencyService = mainComposeDocument.getServiceByName(dependencyName);
            resolveServiceDependsOn(dependencyService);
            service.dependencies.add(dependencyService);
            service.dependencies.addAll(dependencyService.dependencies);
            dependencyService.dependents.add(service);
            if (service.dependencies.contains(service))
                throw new CircularDependsOnException(
                        "Service " + service.name + " is one of its dependencies: " + service.dependencies);
        });
    }
}
