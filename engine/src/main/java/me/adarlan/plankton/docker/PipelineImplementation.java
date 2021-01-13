package me.adarlan.plankton.docker;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.dependencies.WaitFailureOf;
import me.adarlan.plankton.core.dependencies.WaitPort;
import me.adarlan.plankton.core.dependencies.WaitSuccessOf;
import me.adarlan.plankton.logging.Logger;
import me.adarlan.plankton.core.Pipeline;

class PipelineImplementation implements Pipeline {

    final DockerCompose dockerCompose;

    @Getter
    private final String id;

    private final Set<ServiceImplementation> services = new HashSet<>();
    private final Map<String, ServiceImplementation> servicesByName = new HashMap<>();
    private final Map<ServiceImplementation, Map<String, String>> labelsByServiceAndName = new HashMap<>();
    private final Map<Integer, ServiceImplementation> externalPorts = new HashMap<>();

    private final Logger logger = Logger.getLogger();

    PipelineImplementation(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
        this.id = dockerCompose.getProjectName();
        instantiateServices();
        services.forEach(this::initializeServiceLabels);
        services.forEach(this::initializeServiceExpression);
        services.forEach(this::initializeNeedToBuild);
        services.forEach(this::initializeServiceScale);
        services.forEach(this::initializeServiceTimeout);
        services.forEach(this::initializeExternalPorts);
        services.forEach(this::initializeServiceDependencies);
        services.forEach(ServiceImplementation::initializeStatus);
    }

    private void instantiateServices() {
        dockerCompose.getServiceNames().forEach(serviceName -> {
            ServiceImplementation service = new ServiceImplementation(this, serviceName);
            this.services.add(service);
            this.servicesByName.put(serviceName, service);
        });
    }

    private void initializeServiceLabels(ServiceImplementation service) {
        Map<String, String> labelsByName = dockerCompose.getServiceLabelsMap(service.getName());
        labelsByServiceAndName.put(service, labelsByName);
    }

    private void initializeServiceExpression(ServiceImplementation service) {
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            service.setExpression(labelsByName.get(labelName));
        }
    }

    private void initializeNeedToBuild(ServiceImplementation service) {
        Map<String, Object> serviceConfigMap = dockerCompose.getServiceMap(service.getName());
        if (serviceConfigMap.containsKey("build")) {
            service.setNeedToBuild(true);
        } else {
            service.setNeedToBuild(false);
        }
    }

    private void initializeServiceScale(ServiceImplementation service) {
        service.setScale(1);
    }

    private void initializeServiceTimeout(ServiceImplementation service) {
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            service.initializeTimeout(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            service.initializeTimeout(1L, ChronoUnit.MINUTES);
        }
    }

    private void initializeExternalPorts(ServiceImplementation service) {
        List<Map<String, Object>> ports = dockerCompose.getServicePorts(service.getName());
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, service);
        });
    }

    private void initializeServiceDependencies(ServiceImplementation service) {
        Set<ServiceDependency> dependencies = new HashSet<>();
        service.setDependencies(dependencies);
        Map<String, String> labelsByName = labelsByServiceAndName.get(service);
        labelsByName.forEach((labelName, labelValue) -> {

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredServiceName = labelValue;
                ServiceImplementation requiredService = this.getServiceByName(requiredServiceName);
                WaitSuccessOf dependency = new WaitSuccessOf(service, requiredService);
                dependencies.add(dependency);
            }

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredServiceName = labelValue;
                ServiceImplementation requiredService = this.getServiceByName(requiredServiceName);
                WaitFailureOf dependency = new WaitFailureOf(service, requiredService);
                dependencies.add(dependency);
            }

            else if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                ServiceImplementation requiredService = externalPorts.get(port);
                WaitPort dependency = new WaitPort(service, requiredService, port);
                dependencies.add(dependency);
            }
        });
    }

    @Override
    public void run() throws InterruptedException {
        boolean done = false;
        while (!done) {
            done = true;
            for (ServiceImplementation service : services) {
                service.refresh();
                if (!service.hasEnded()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info(() -> "Pipeline finished");
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
}