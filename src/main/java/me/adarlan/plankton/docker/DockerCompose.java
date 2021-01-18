package me.adarlan.plankton.docker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ComposeDocument;

public class DockerCompose implements Compose {

    private final DockerDaemon dockerDaemon;
    private final ComposeDocument composeDocument;
    private final String metadataDirectoryPath;

    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    private boolean networkCreated = false;

    private final Set<String> runningContainers = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DockerCompose(DockerComposeConfiguration configuration) {
        this.dockerDaemon = configuration.dockerDaemon();
        this.composeDocument = configuration.composeDocument();
        this.metadataDirectoryPath = configuration.metadataDirectoryPath();
        this.initializeOptions();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void initializeOptions() {
        List<String> list = new ArrayList<>();
        list.add("--no-ansi");
        list.add("--project-name " + composeDocument.getProjectName());
        list.add("--file " + composeDocument.getFilePath());
        list.add("--project-directory " + composeDocument.getProjectDirectory());
        options = list.stream().collect(Collectors.joining(" "));
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

    private void createNetwork() {
        BashScript script = createScript("createNetwork");
        String networkName = composeDocument.getProjectName() + "_default";
        script.command("docker network create --attachable " + networkName);
        script.runSuccessfully();
    }

    @Override
    public boolean createContainers(String serviceName, int serviceScale, Consumer<String> forEachOutput,
            Consumer<String> forEachError) {
        synchronized (this) {
            if (!networkCreated) {
                createNetwork();
                networkCreated = true;
            }
        }
        final BashScript script = createScript("createContainers_" + serviceName);
        final String upOptions = "--no-start --scale " + serviceName + "=" + serviceScale;
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + serviceName);
        script.forEachOutput(forEachOutput);
        script.forEachError(message -> {
            String m = message.trim();
            if (m.equals("Creating " + composeDocument.getProjectName() + "_" + serviceName + "_1 ...")
                    || m.equals("Creating " + composeDocument.getProjectName() + "_" + serviceName + "_1 ... done")) {
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
    public void startContainer(String containerName, Consumer<String> forEachOutput, Consumer<String> forEachError) {
        final BashScript script = createScript("runContainer_" + containerName);
        script.command("docker container start --attach " + containerName);
        script.forEachOutput(forEachOutput);
        script.forEachError(forEachError);
        if (shutdown) {
            return;
        }
        runningContainers.add(containerName);
        script.run();
    }

    @Override
    public DockerContainerState getContainerState(String containerName) {
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = createScript("getContainerState_" + containerName);
        String d = metadataDirectoryPath + "/containers";
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
        DockerContainerState containerState = parseContainerStateJson(json);
        if (containerState.exited()) {
            runningContainers.remove(containerName);
        }
        return containerState;
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
        logger.info("DockerCompose stop container: {}", containerName);
        BashScript script = createScript("stopContainer_" + containerName);
        script.command("docker container stop " + containerName);
        script.run();
    }

    @Override
    public boolean killContainer(String containerName) {
        logger.info("DockerCompose kill container: {}", containerName);
        BashScript script = createScript("killContainer_" + containerName);
        script.command("docker container kill " + containerName);
        script.run();
        return script.getExitCode() == 0;
    }

    private BashScript createScript(String name) {
        BashScript script = new BashScript(name);
        script.env("DOCKER_HOST=" + dockerDaemon.getSocketAddress());
        return script;
    }

    @Override
    public ComposeDocument getDocument() {
        return composeDocument;
    }

    private boolean shutdown = false;

    private void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        runningContainers.forEach(this::killContainer);
    }
}