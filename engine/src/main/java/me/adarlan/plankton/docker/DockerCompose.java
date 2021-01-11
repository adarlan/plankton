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
import me.adarlan.plankton.core.Logger;

public class DockerCompose {

    @Getter
    private final String filePath;

    @Getter
    private final String projectName;

    @Getter
    private final String projectDirectory;

    @Getter
    private final String metadataDirectoryPath;

    private final String dockerHostVariable;
    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    private Map<String, Object> document;
    private Map<String, Object> services;
    private final Set<String> serviceNames = new HashSet<>();

    private final Logger logger = Logger.getLogger();

    public DockerCompose(DockerPipelineConfig config) {
        this.projectName = config.getPipelineId();
        this.projectDirectory = config.getWorkspaceDirectoryPath();
        this.metadataDirectoryPath = config.getMetadataDirectoryPath() + "/" + config.getPipelineId();
        this.filePath = metadataDirectoryPath + "/docker-compose.yml";
        if (config.getDockerHost() == null) {
            this.dockerHostVariable = "DOCKER_HOST=unix:///var/run/docker.sock";
        } else {
            this.dockerHostVariable = "DOCKER_HOST=" + config.getDockerHost();
        }
        this.initializeMetadataDirectory();
        this.initializeFileFromOriginalFile(config.getComposeFilePath());
        this.initializeOptions();
        this.createNetwork();
        this.initializeDocument();
    }

    private void initializeMetadataDirectory() {
        BashScript script = new BashScript("initializeMetadata");
        script.command("mkdir -p " + metadataDirectoryPath);
        script.runSuccessfully();
    }

    private void initializeFileFromOriginalFile(String originalFile) {
        BashScript script = new BashScript("initializeFileFromOriginalFile");
        script.command(BASE_COMMAND + " --file " + originalFile + " config > " + filePath);
        script.command("cat " + filePath);
        script.runSuccessfully();
    }

    private void initializeOptions() {
        List<String> list = new ArrayList<>();
        list.add("--no-ansi");
        list.add("--project-name " + projectName);
        list.add("--file " + filePath);
        list.add("--project-directory " + projectDirectory);
        options = list.stream().collect(Collectors.joining(" "));
    }

    private void createNetwork() {
        BashScript script = new BashScript("createNetwork");
        String networkName = projectName + "_default";
        script.env(dockerHostVariable);
        script.command("docker network create --attachable " + networkName);
        script.runSuccessfully();
    }

    @SuppressWarnings("unchecked")
    private void initializeDocument() {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);) {
            final Yaml yaml = new Yaml();
            document = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new PlanktonDockerException("Unable to initialize the Docker Compose document", e);
        }
        services = (Map<String, Object>) document.get("services");
        services.keySet().forEach(serviceNames::add);
    }

    public Set<String> getServiceNames() {
        return Collections.unmodifiableSet(serviceNames);
    }

    public boolean buildImage(JobImplementation job) {
        final BashScript script = new BashScript("buildImage_" + job.getName());
        final String buildOptions = "";
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " build " + buildOptions + " " + job.getName());
        script.forEachOutputAndError(job::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean pullImage(JobImplementation job) {
        final BashScript script = new BashScript("pullImage_" + job.getName());
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " pull " + job.getName());
        script.forEachOutputAndError(job::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean createContainers(JobImplementation job) {
        final BashScript script = new BashScript("createContainers_" + job.getName());
        final String upOptions = "--no-start --scale " + job.getName() + "=" + job.getScale();
        script.env(dockerHostVariable);
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + job.getName());
        script.forEachOutputAndError(job::log);
        script.run();
        return script.getExitCode() == 0;
    }

    void runContainer(JobInstanceImplementation jobInstance) {
        final BashScript script = new BashScript("runContainer_" + jobInstance.getContainerName());
        script.env(dockerHostVariable);
        script.command("docker container start --attach " + jobInstance.getContainerName());
        script.forEachOutputAndError(jobInstance::log);
        script.run();
    }

    ContainerState getContainerState(JobInstanceImplementation jobInstance) {
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = new BashScript("getContainerState_" + jobInstance.getContainerName());
        script.env(dockerHostVariable);
        String d = metadataDirectoryPath + "/containers";
        String f = d + "/" + jobInstance.getContainerName();
        script.command("mkdir -p " + d);
        script.command("docker container inspect " + jobInstance.getContainerName() + " > " + f);
        script.command("cat " + f + " | jq --compact-output '.[] | .State'");
        script.command("STATUS=$(cat " + f + " | jq -r '.[] | .State.Status')");
        script.command("cat " + f + " > " + f + ".$STATUS");
        script.forEachOutput(line -> {
            scriptOutput.add(line);
            logger.debug(() -> script.getName() + " >> " + line);
        });
        script.runSuccessfully();
        final String json = scriptOutput.stream().collect(Collectors.joining());
        return parseContainerStateJson(json);
    }

    private ContainerState parseContainerStateJson(String json) {
        try {
            return new ObjectMapper().readValue(json, ContainerState.class);
        } catch (JsonProcessingException e) {
            throw new PlanktonDockerException("Unable to parse container state JSON", e);
        }
    }

    void stopContainer(JobInstanceImplementation jobInstance) {
        BashScript script = new BashScript("stopContainer_" + jobInstance.getContainerName());
        script.env(dockerHostVariable);
        script.command("docker container stop " + jobInstance.getContainerName());
        script.run();
    }

    boolean killContainer(JobInstanceImplementation jobInstance) {
        BashScript script = new BashScript("killContainer_" + jobInstance.getContainerName());
        script.env(dockerHostVariable);
        script.command("docker container kill " + jobInstance.getContainerName());
        script.run();
        return script.getExitCode() == 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getService(String serviceName) {
        return (Map<String, Object>) services.get(serviceName);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getServiceLabels(String serviceName) {
        Map<String, Object> service = getService(serviceName);
        String key = "labels";
        if (service.containsKey(key)) {
            return (Map<String, String>) service.get(key);
        } else {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getServicePorts(String serviceName) {
        Map<String, Object> service = getService(serviceName);
        String key = "ports";
        if (service.containsKey(key)) {
            return (List<Map<String, Object>>) service.get(key);
        } else {
            return new ArrayList<>();
        }
    }
}