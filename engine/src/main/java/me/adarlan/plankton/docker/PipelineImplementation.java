package me.adarlan.plankton.docker;

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

import lombok.Getter;
import lombok.NonNull;

import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceStatus;
import me.adarlan.plankton.core.dependencies.WaitFailureOf;
import me.adarlan.plankton.core.dependencies.WaitPort;
import me.adarlan.plankton.core.dependencies.WaitSuccessOf;
import me.adarlan.plankton.logging.Colors;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.core.Pipeline;

class PipelineImplementation implements Pipeline {

    final DockerCompose dockerCompose;

    @Getter
    private final String id;

    private final Set<ServiceImplementation> services = new HashSet<>();
    private final Map<String, ServiceImplementation> servicesByName = new HashMap<>();
    private final Map<ServiceImplementation, Map<String, String>> labelsByServiceAndName = new HashMap<>();
    private final Map<Integer, ServiceImplementation> externalPorts = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Integer biggestServiceNameLength;

    PipelineImplementation(DockerCompose dockerCompose) {
        logger.trace("PipelineImplementation");
        this.dockerCompose = dockerCompose;
        this.id = dockerCompose.getProjectName();
        instantiateServices();
        services.forEach(this::initializeServiceLabels);
        services.forEach(this::initializeServiceExpression);
        services.forEach(this::initializeNeedToBuild);
        services.forEach(this::initializeServiceScaleAndInstances);
        services.forEach(this::initializeServiceTimeout);
        services.forEach(this::initializeExternalPorts);
        services.forEach(this::initializeServiceDependencies);
        services.forEach(this::initializeServiceStatus);
        this.initializeBiggestServiceNameLength();
        this.initializeServiceColors();
    }

    private void instantiateServices() {
        logger.trace("instantiateServices");
        dockerCompose.getServiceNames().forEach(serviceName -> {
            ServiceImplementation service = new ServiceImplementation(this, serviceName);
            this.services.add(service);
            this.servicesByName.put(serviceName, service);
        });
    }

    private void initializeServiceLabels(ServiceImplementation service) {
        logger.trace("initializeServiceLabels: {}", service.name);
        Map<String, String> labelsByName = dockerCompose.getServiceLabelsMap(service.name);
        labelsByServiceAndName.put(service, labelsByName);
    }

    private void initializeServiceExpression(ServiceImplementation service) {
        logger.trace("initializeServiceExpression: {}", service.name);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            service.expression = labelsByName.get(labelName);
        }
    }

    private void initializeNeedToBuild(ServiceImplementation service) {
        logger.trace("initializeNeedToBuild: {}", service.name);
        Map<String, Object> serviceConfigMap = dockerCompose.getServiceMap(service.name);
        if (serviceConfigMap.containsKey("build")) {
            service.needToBuild = true;
        } else {
            service.needToBuild = false;
        }
    }

    private void initializeServiceScaleAndInstances(ServiceImplementation service) {
        logger.trace("initializeServiceScaleAndInstances: {}", service.name);

        int scale = 1;
        // TODO read from compose document

        service.scale = scale;
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            ServiceInstanceImplementation instance = new ServiceInstanceImplementation(service, instanceNumber);
            service.instances.add(instance);
        }
    }

    private void initializeServiceTimeout(ServiceImplementation service) {
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

    private void initializeExternalPorts(ServiceImplementation service) {
        logger.trace("initializeExternalPorts: {}", service.name);
        List<Map<String, Object>> ports = dockerCompose.getServicePorts(service.name);
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, service);
        });
    }

    private void initializeServiceDependencies(ServiceImplementation service) {
        logger.trace("initializeServiceDependencies: {}", service.name);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        labelsByName.forEach((labelName, labelValue) -> {

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredServiceName = labelValue;
                ServiceImplementation requiredService = this.getServiceByName(requiredServiceName);
                WaitSuccessOf dependency = new WaitSuccessOf(service, requiredService);
                service.dependencies.add(dependency);
            }

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredServiceName = labelValue;
                ServiceImplementation requiredService = this.getServiceByName(requiredServiceName);
                WaitFailureOf dependency = new WaitFailureOf(service, requiredService);
                service.dependencies.add(dependency);
            }

            else if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                ServiceImplementation requiredService = externalPorts.get(port);
                WaitPort dependency = new WaitPort(service, requiredService, port);
                service.dependencies.add(dependency);
            }
        });
    }

    private void initializeServiceStatus(ServiceImplementation service) {
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

    private void evaluateExpression(ServiceImplementation service) {
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

    private void initializeBiggestServiceNameLength() {
        logger.trace("initializeBiggestServiceNameLength");
        biggestServiceNameLength = 0;
        for (ServiceImplementation service : getEnabledServices()) {
            for (int i = 1; i <= service.getScale(); i++) {
                String name = service.name + "_" + i;
                int len = name.length();
                if (len > biggestServiceNameLength) {
                    biggestServiceNameLength = len;
                }
            }
        }
    }

    private void initializeServiceColors() {
        logger.trace("initializeServiceColors");
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_RED);
        list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_YELLOW);
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_PURPLE);
        list.add(Colors.BRIGHT_CYAN);
        int serviceIndex = 0;
        for (ServiceImplementation service : getEnabledServices()) {
            int colorIndex = serviceIndex % list.size();
            String color;
            color = list.get(colorIndex);
            serviceIndex++;
            service.color = color;
            service.colorizedName = color + service.name + Colors.ANSI_RESET;
            for (ServiceInstanceImplementation instance : service.instances) {
                if (service.getScale() == 1) {
                    instance.name = service.name;
                } else {
                    instance.name = service.name + "_" + instance.number;
                }
                instance.colorizedName = service.color + instance.name + Colors.ANSI_RESET;
            }
        }
    }

    @Override
    public void run() throws InterruptedException {
        logger.info("Pipeline running");
        boolean done = false;
        while (!done) {
            done = true;
            for (ServiceImplementation service : getEnabledServices()) {
                service.refresh();
                if (!service.hasEnded()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info("Pipeline finished");
    }

    @Override
    public Set<Service> getServices() {
        return Collections.unmodifiableSet(services);
    }

    @Override
    public ServiceImplementation getServiceByName(@NonNull String serviceName) {
        if (!servicesByName.containsKey(serviceName))
            throw new PlanktonDockerException("Service not found: " + serviceName);
        return servicesByName.get(serviceName);
    }

    public Set<ServiceImplementation> getEnabledServices() {
        return Collections.unmodifiableSet(services.stream().filter(service -> service.status != ServiceStatus.DISABLED)
                .collect(Collectors.toSet()));
    }
}