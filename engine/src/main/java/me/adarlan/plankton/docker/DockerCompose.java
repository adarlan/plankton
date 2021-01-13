package me.adarlan.plankton.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.adarlan.plankton.logging.Logger;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;

public class DockerCompose extends Compose {

    private final String dockerHost;
    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    private final Logger logger = Logger.getLogger();

    public DockerCompose(DockerComposeConfiguration configuration) {
        super(configuration);
        this.dockerHost = configuration.dockerHost();
        this.initializeOptions();
        this.createNetwork();
    }

    private void initializeOptions() {
        List<String> list = new ArrayList<>();
        list.add("--no-ansi");
        list.add("--project-name " + getProjectName());
        list.add("--file " + getFilePath());
        list.add("--project-directory " + getProjectDirectory());
        options = list.stream().collect(Collectors.joining(" "));
    }

    private void createNetwork() {
        BashScript script = createScript("createNetwork");
        String networkName = getProjectName() + "_default";
        script.command("docker network create --attachable " + networkName);
        script.runSuccessfully();
    }

    public boolean buildImage(ServiceImplementation service) {
        final BashScript script = createScript("buildImage_" + service.getName());
        final String buildOptions = "";
        script.command(BASE_COMMAND + " " + options + " build " + buildOptions + " " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean pullImage(ServiceImplementation service) {
        final BashScript script = createScript("pullImage_" + service.getName());
        script.command(BASE_COMMAND + " " + options + " pull " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    public boolean createContainers(ServiceImplementation service) {
        final BashScript script = createScript("createContainers_" + service.getName());
        final String upOptions = "--no-start --scale " + service.getName() + "=" + service.getScale();
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + service.getName());
        script.forEachOutputAndError(service::log);
        script.run();
        return script.getExitCode() == 0;
    }

    void runContainer(ServiceInstanceImplementation serviceInstance) {
        final BashScript script = createScript("runContainer_" + serviceInstance.getContainerName());
        script.command("docker container start --attach " + serviceInstance.getContainerName());
        script.forEachOutputAndError(serviceInstance::log);
        script.run();
    }

    ContainerState getContainerState(ServiceInstanceImplementation serviceInstance) {
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = createScript("getContainerState_" + serviceInstance.getContainerName());
        String d = getMetadataDirectory() + "/containers";
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
        BashScript script = createScript("stopContainer_" + serviceInstance.getContainerName());
        script.command("docker container stop " + serviceInstance.getContainerName());
        script.run();
    }

    boolean killContainer(ServiceInstanceImplementation serviceInstance) {
        BashScript script = createScript("killContainer_" + serviceInstance.getContainerName());
        script.command("docker container kill " + serviceInstance.getContainerName());
        script.run();
        return script.getExitCode() == 0;
    }

    private BashScript createScript(String name) {
        BashScript script = new BashScript(name);
        script.env("DOCKER_HOST=" + dockerHost);
        script.forEachVariable(variable -> logger.debug(() -> script.getName() + " | " + variable));
        script.forEachCommand(command -> logger.debug(() -> script.getName() + " | " + command));
        script.forEachOutput(output -> logger.debug(() -> script.getName() + " >> " + output));
        script.forEachError(error -> logger.error(() -> script.getName() + " >> " + error));
        return script;
    }
}