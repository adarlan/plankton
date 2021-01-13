package me.adarlan.plankton.docker;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import lombok.experimental.Delegate;
import me.adarlan.plankton.core.Logger;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ComposeConfiguration;

public class DockerCompose {

    @Delegate
    private final Compose compose;

    // @Getter
    // private final String filePath;

    // @Getter
    // private final String projectName;

    // @Getter
    // private final String projectDirectory;

    // @Getter
    // private final String metadataDirectoryPath;

    private final String dockerHostVariable;
    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    // private Map<String, Object> documentConfigMap;
    // private Map<String, Object> servicesConfigMap;
    // private final Set<String> serviceNames = new HashSet<>();

    private final Logger logger = Logger.getLogger();

    public DockerCompose(DockerPipelineConfig pipelineConfig) {
        this.compose = newCompose(pipelineConfig);
        // this.projectName = pipelineConfig.getPipelineId();
        // this.projectDirectory = pipelineConfig.getWorkspaceDirectoryPath();
        // this.metadataDirectoryPath = pipelineConfig.getMetadataDirectoryPath() + "/" + pipelineConfig.getPipelineId();
        // this.filePath = metadataDirectoryPath + "/docker-compose.yml";
        if (pipelineConfig.getDockerHost() == null) {
            this.dockerHostVariable = "DOCKER_HOST=unix:///var/run/docker.sock";
        } else {
            this.dockerHostVariable = "DOCKER_HOST=" + pipelineConfig.getDockerHost();
        }
        // this.initializeMetadataDirectory();
        // this.initializeFileFromOriginalFile(pipelineConfig.getComposeFilePath());
        this.initializeOptions();
        this.createNetwork();
        // this.initializeDocument();
    }

    private Compose newCompose(DockerPipelineConfig pipelineConfig) {
        return new Compose(new ComposeConfiguration() {

            @Override
            public String projectName() {
                return pipelineConfig.getPipelineId();
            }

            @Override
            public String filePath() {
                return pipelineConfig.getComposeFilePath();
            }

            @Override
            public String projectDirectory() {
                return pipelineConfig.getWorkspaceDirectoryPath();
            }

            @Override
            public String metadataDirectory() {
                return pipelineConfig.getMetadataDirectoryPath() + "/" + pipelineConfig.getPipelineId();
            }
        });
    }

    // private void initializeMetadataDirectory() {
    //     BashScript script = Utils.createScript("initializeMetadata", logger);
    //     script.command("mkdir -p " + metadataDirectoryPath);
    //     script.runSuccessfully();
    // }

    // private void initializeFileFromOriginalFile(String originalFile) {
    //     BashScript script = Utils.createScript("initializeFileFromOriginalFile", logger);
    //     script.command(BASE_COMMAND + " --file " + originalFile + " config > " + filePath);
    //     script.command("cat " + filePath);
    //     script.runSuccessfully();
    // }

    private void initializeOptions() {
        List<String> list = new ArrayList<>();
        list.add("--no-ansi");
        list.add("--project-name " + compose.getProjectName());
        list.add("--file " + compose.getFilePath());
        list.add("--project-directory " + compose.getProjectDirectory());
        options = list.stream().collect(Collectors.joining(" "));
    }

    private void createNetwork() {
        BashScript script = Utils.createScript("createNetwork", logger);
        String networkName = compose.getProjectName() + "_default";
        script.env(dockerHostVariable);
        script.command("docker network create --attachable " + networkName);
        script.runSuccessfully();
    }

    // @SuppressWarnings("unchecked")
    // private void initializeDocument() {
    //     try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
    //         final Yaml yaml = new Yaml();
    //         documentConfigMap = yaml.load(fileInputStream);
    //     } catch (IOException e) {
    //         throw new PlanktonDockerException("Unable to initialize the Docker Compose document", e);
    //     }
    //     servicesConfigMap = (Map<String, Object>) documentConfigMap.get("services");
    //     servicesConfigMap.keySet().forEach(serviceNames::add);
    // }

    // public Set<String> getServiceNames() {
    //     return Collections.unmodifiableSet(serviceNames);
    // }

    public boolean buildImage(ServiceImplementation service) {
        final BashScript script = Utils.createScript("buildImage_" + service.getName(), logger);
        final String buildOptions = "";
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " build " + buildOptions + " " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean pullImage(ServiceImplementation service) {
        final BashScript script = Utils.createScript("pullImage_" + service.getName(), logger);
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " pull " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean createContainers(ServiceImplementation service) {
        final BashScript script = Utils.createScript("createContainers_" + service.getName(), logger);
        final String upOptions = "--no-start --scale " + service.getName() + "=" + service.getScale();
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    void runContainer(ServiceInstanceImplementation serviceInstance) {
        final BashScript script = Utils.createScript("runContainer_" + serviceInstance.getContainerName(), logger);
        script.env(dockerHostVariable);
        script.command("docker container start --attach " + serviceInstance.getContainerName());
        script.forEachOutputAndError(serviceInstance::log);
        script.run();
    }

    ContainerState getContainerState(ServiceInstanceImplementation serviceInstance) {
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = Utils.createScript("getContainerState_" + serviceInstance.getContainerName(), logger);
        script.env(dockerHostVariable);
        String d = compose.getMetadataDirectory() + "/containers";
        String f = d + "/" + serviceInstance.getContainerName();
        script.command("mkdir -p " + d);
        script.command("docker container inspect " + serviceInstance.getContainerName() + " > " + f);
        script.command("cat " + f + " | jq --compact-output '.[] | .State'");
        script.command("STATUS=$(cat " + f + " | jq -r '.[] | .State.Status')");
        script.command("cat " + f + " > " + f + ".$STATUS");
        script.forEachOutput(scriptOutput::add);
        script.runSuccessfully();
        final String json = scriptOutput.stream().collect(Collectors.joining());
        logger.debug(() -> serviceInstance.getContainerName() + " container state JSON: " + json);
        return parseContainerStateJson(json);
    }

    private ContainerState parseContainerStateJson(String json) {
        try {
            return new ObjectMapper().readValue(json, ContainerState.class);
        } catch (JsonProcessingException e) {
            throw new PlanktonDockerException("Unable to parse container state JSON", e);
        }
    }

    void stopContainer(ServiceInstanceImplementation serviceInstance) {
        BashScript script = Utils.createScript("stopContainer_" + serviceInstance.getContainerName(), logger);
        script.env(dockerHostVariable);
        script.command("docker container stop " + serviceInstance.getContainerName());
        script.run();
    }

    boolean killContainer(ServiceInstanceImplementation serviceInstance) {
        BashScript script = Utils.createScript("killContainer_" + serviceInstance.getContainerName(), logger);
        script.env(dockerHostVariable);
        script.command("docker container kill " + serviceInstance.getContainerName());
        script.run();
        return script.getExitCode() == 0;
    }

    // @SuppressWarnings("unchecked")
    // public Map<String, Object> getServiceConfigMap(String serviceName) {
    //     return (Map<String, Object>) servicesConfigMap.get(serviceName);
    // }

    // @SuppressWarnings("unchecked")
    // public Map<String, String> getServiceLabels(String serviceName) {
    //     Map<String, Object> service = getServiceConfigMap(serviceName);
    //     String key = "labels";
    //     if (service.containsKey(key)) {
    //         return (Map<String, String>) service.get(key);
    //     } else {
    //         return new HashMap<>();
    //     }
    // }

    // @SuppressWarnings("unchecked")
    // public List<Map<String, Object>> getServicePorts(String serviceName) {
    //     Map<String, Object> service = getServiceConfigMap(serviceName);
    //     String key = "ports";
    //     if (service.containsKey(key)) {
    //         return (List<Map<String, Object>>) service.get(key);
    //     } else {
    //         return new ArrayList<>();
    //     }
    // }
}