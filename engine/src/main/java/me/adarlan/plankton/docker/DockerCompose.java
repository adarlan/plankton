package me.adarlan.plankton.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;

public class DockerCompose extends Compose {

    private final String dockerHost;
    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    @Override
    public boolean buildImage(String serviceName, Consumer<String> forEachOutput, Consumer<String> forEachError) {
        final BashScript script = createScript("buildImage_" + serviceName);
        final String buildOptions = "";
        script.command(BASE_COMMAND + " " + options + " build " + buildOptions + " " + serviceName);
        script.forEachOutput(forEachOutput);
        script.forEachError(message -> {
            String m = message.trim();
            if (!m.equals("Building " + serviceName)) {
                // TODO test if it is the first message?
                forEachError.accept(message);
            }
        });
        script.run();
        return script.getExitCode() == 0;
    }

    @Override
    public boolean pullImage(String serviceName, Consumer<String> forEachOutput, Consumer<String> forEachError) {
        final BashScript script = createScript("pullImage_" + serviceName);
        script.command(BASE_COMMAND + " " + options + " pull " + serviceName);
        script.forEachOutput(forEachOutput);
        script.forEachError(message -> {
            String m = message.trim();
            if (m.startsWith("Pulling " + serviceName + " ...")) {
                forEachOutput.accept(message);
            } else {
                forEachError.accept(message);
            }
        });
        script.run();
        return script.getExitCode() == 0;
    }

    @Override
    public boolean createContainers(String serviceName, int serviceScale, Consumer<String> forEachOutput,
            Consumer<String> forEachError) {
        final BashScript script = createScript("createContainers_" + serviceName);
        final String upOptions = "--no-start --scale " + serviceName + "=" + serviceScale;
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + serviceName);
        script.forEachOutput(forEachOutput);
        script.forEachError(message -> {
            String m = message.trim();
            if (m.equals("Creating " + getProjectName() + "_" + serviceName + "_1 ...")
                    || m.equals("Creating " + getProjectName() + "_" + serviceName + "_1 ... done")) {
                // TODO replace _1 by instance numbers
                forEachOutput.accept(message);
            } else {
                forEachError.accept(message);
            }
        });
        script.run();
        return script.getExitCode() == 0;
    }

    @Override
    public void runContainer(String containerName, Consumer<String> forEachOutput, Consumer<String> forEachError) {
        final BashScript script = createScript("runContainer_" + containerName);
        script.command("docker container start --attach " + containerName);
        script.forEachOutput(forEachOutput);
        script.forEachError(forEachError);
        script.run();
    }

    @Override
    public DockerContainerState getContainerState(String containerName) {
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = createScript("getContainerState_" + containerName);
        String d = getMetadataDirectory() + "/containers";
        String f = d + "/" + containerName;
        script.command("mkdir -p " + d);
        script.command("docker container inspect " + containerName + " > " + f);
        script.command("cat " + f + " | jq --compact-output '.[] | .State'");
        script.command("STATUS=$(cat " + f + " | jq -r '.[] | .State.Status')");
        script.command("cat " + f + " > " + f + ".$STATUS");
        script.forEachOutput(scriptOutput::add);
        script.runSuccessfully();
        final String json = scriptOutput.stream().collect(Collectors.joining());
        logger.debug("{}: {}", containerName, json);
        return parseContainerStateJson(json);
    }

    private DockerContainerState parseContainerStateJson(String json) {
        try {
            return new ObjectMapper().readValue(json, DockerContainerState.class);
        } catch (JsonProcessingException e) {
            throw new DockerComposeException("Unable to parse container state JSON", e);
        }
    }

    @Override
    public void stopContainer(String containerName) {
        BashScript script = createScript("stopContainer_" + containerName);
        script.command("docker container stop " + containerName);
        script.run();
    }

    private BashScript createScript(String name) {
        BashScript script = new BashScript(name);
        script.env("DOCKER_HOST=" + dockerHost);
        return script;
    }
}