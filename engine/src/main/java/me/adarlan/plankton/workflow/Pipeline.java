package me.adarlan.plankton.workflow;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import me.adarlan.plankton.workflow.dependencies.WaitFailureOf;
import me.adarlan.plankton.workflow.dependencies.WaitPort;
import me.adarlan.plankton.workflow.dependencies.WaitSuccessOf;
import me.adarlan.plankton.logging.Colors;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;

@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class Pipeline {

    final Compose compose;

    @Getter
    private final String id;

    private final Set<Service> services = new HashSet<>();
    private final Map<String, Service> servicesByName = new HashMap<>();
    private final Map<Service, Map<String, String>> labelsByServiceAndName = new HashMap<>();
    private final Map<Integer, Service> externalPorts = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    Integer biggestServiceNameLength;

    public Pipeline(Compose compose) {
        logger.trace("Pipeline");
        this.compose = compose;
        this.id = compose.getProjectName();
        instantiateServices();
        services.forEach(this::initializeServiceLabels);
        services.forEach(this::initializeServiceExpression);
        services.forEach(this::initializeNeedToBuild);
        services.forEach(this::initializeServiceScaleAndInstances);
        services.forEach(this::initializeServiceTimeout);
        services.forEach(this::initializeExternalPorts);
        services.forEach(this::initializeServiceDependencies);
        services.forEach(this::initializeServiceStatus);
        this.initializeInstanceNamesAndBiggestName();
        this.initializeColors();
    }

    private void instantiateServices() {
        logger.trace("instantiateServices");
        compose.getServiceNames().forEach(serviceName -> {
            Service service = new Service(this, serviceName);
            this.services.add(service);
            this.servicesByName.put(serviceName, service);
        });
    }

    private void initializeServiceLabels(Service service) {
        logger.trace("initializeServiceLabels: {}", service.name);
        Map<String, String> labelsByName = compose.getServiceLabelsMap(service.name);
        labelsByServiceAndName.put(service, labelsByName);
    }

    private void initializeServiceExpression(Service service) {
        logger.trace("initializeServiceExpression: {}", service.name);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            service.expression = labelsByName.get(labelName);
        }
    }

    private void initializeNeedToBuild(Service service) {
        logger.trace("initializeNeedToBuild: {}", service.name);
        Map<String, Object> serviceConfigMap = compose.getServiceMap(service.name);
        if (serviceConfigMap.containsKey("build")) {
            service.needToBuild = true;
        } else {
            service.needToBuild = false;
        }
    }

    private void initializeServiceScaleAndInstances(Service service) {
        logger.trace("initializeServiceScaleAndInstances: {}", service.name);

        int scale = 1;
        // TODO read from compose document

        service.scale = scale;
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            ServiceInstance instance = new ServiceInstance(service, instanceNumber);
            service.instances.add(instance);
        }
    }

    private void initializeServiceTimeout(Service service) {
        logger.trace("initializeServiceTimeout: {}", service.name);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            service.timeoutLimit = Duration.of(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            service.timeoutLimit = Duration.of(1L, ChronoUnit.MINUTES);
        }
    }

    private void initializeExternalPorts(Service service) {
        logger.trace("initializeExternalPorts: {}", service.name);
        List<Map<String, Object>> ports = compose.getServicePorts(service.name);
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, service);
        });
    }

    private void initializeServiceDependencies(Service service) {
        logger.trace("initializeServiceDependencies: {}", service.name);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        labelsByName.forEach((labelName, labelValue) -> {

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredServiceName = labelValue;
                Service requiredService = this.getServiceByName(requiredServiceName);
                WaitSuccessOf dependency = new WaitSuccessOf(service, requiredService);
                service.dependencies.add(dependency);
            }

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredServiceName = labelValue;
                Service requiredService = this.getServiceByName(requiredServiceName);
                WaitFailureOf dependency = new WaitFailureOf(service, requiredService);
                service.dependencies.add(dependency);
            }

            else if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                Service requiredService = externalPorts.get(port);
                WaitPort dependency = new WaitPort(service, requiredService, port);
                service.dependencies.add(dependency);
            }
        });
    }

    private void initializeServiceStatus(Service service) {
        logger.trace("initializeServiceStatus: {}", service.name);
        if (service.expression != null) {
            evaluateExpression(service);
            if (service.expressionResult) {
                service.status = ServiceStatus.WAITING;
                logger.info("{} -> Enabled by expression: {}", service.name, service.expression);
            } else {
                service.status = ServiceStatus.DISABLED;
                logger.info("{} -> Disabled by expression: {}", service.name, service.expression);
            }
        } else {
            service.status = ServiceStatus.WAITING;
        }
    }

    private void evaluateExpression(Service service) {
        logger.trace("evaluateExpression: {}", service.name);

        final String scriptName = "evaluateExpression_" + service.name;
        BashScript script = new BashScript(scriptName);
        script.command(service.expression);
        script.run();
        // TODO do it inside a sandbox container to prevent script injection
        // TODO it needs timeout
        // TODO add variables

        if (script.getExitCode() == 0) {
            service.expressionResult = true;
        } else {
            service.expressionResult = false;
        }
    }

    private void initializeInstanceNamesAndBiggestName() {
        logger.trace("initializeInstanceNamesAndBiggestName");
        biggestServiceNameLength = 0;
        for (Service service : getEnabledServices()) {
            for (ServiceInstance instance : service.instances) {
                if (service.getScale() == 1) {
                    instance.name = service.name;
                } else {
                    instance.name = service.name + "_" + instance.number;
                }
                int len = instance.name.length();
                if (len > biggestServiceNameLength) {
                    biggestServiceNameLength = len;
                }
            }
        }
    }

    private void initializeColors() {
        logger.trace("initializeColors");
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_RED);
        list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_YELLOW);
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_PURPLE);
        list.add(Colors.BRIGHT_CYAN);
        int serviceIndex = 0;
        for (Service service : getEnabledServices()) {
            int colorIndex = serviceIndex % list.size();
            service.color = list.get(colorIndex);
            serviceIndex++;
            service.infoPrefix = Utils.infoPrefixOf(service);
            service.logPrefix = Utils.logPrefixOf(service);
            for (ServiceInstance instance : service.instances) {
                instance.logPrefix = Utils.logPrefixOf(instance);
            }
        }
    }

    public void run() throws InterruptedException {
        logger.info("Pipeline running");
        boolean done = false;
        while (!done) {
            done = true;
            for (Service service : getWaitingOrRunningServices()) {
                service.refresh();
                if (service.isWaitingOrRunning()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info("Pipeline finished");
    }

    public Set<Service> getServices() {
        return Collections.unmodifiableSet(services);
    }

    public Service getServiceByName(@NonNull String serviceName) {
        if (!servicesByName.containsKey(serviceName))
            throw new PlanktonDockerException("Service not found: " + serviceName);
        return servicesByName.get(serviceName);
    }

    public Set<Service> getEnabledServices() {
        return Collections.unmodifiableSet(services.stream().filter(Service::isEnabled).collect(Collectors.toSet()));
    }

    public Set<Service> getWaitingOrRunningServices() {
        return Collections
                .unmodifiableSet(services.stream().filter(Service::isWaitingOrRunning).collect(Collectors.toSet()));
    }
}
