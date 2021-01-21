package me.adarlan.plankton.docker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;
import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.compose.ComposeDocument;

@ToString(of = { "dockerDaemon", "composeDocument" })
public class DockerAdapter implements ComposeAdapter {

    private final DockerDaemon dockerDaemon;
    private final ComposeDocument composeDocument;
    private final String containerStateDirectoryPath;

    private final String projectName;

    private static final String BASE_COMMAND = "docker-compose";
    private String options;

    private boolean networkCreated = false;

    private final Set<String> runningContainers = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String LOADING = "Loading DockerAdapter ... ";

    public DockerAdapter(DockerAdapterConfiguration configuration) {

        logger.info(LOADING);

        this.dockerDaemon = configuration.dockerDaemon();
        this.composeDocument = configuration.composeDocument();
        this.containerStateDirectoryPath = configuration.containerStateDirectoryPath();
        this.projectName = composeDocument.projectName();

        logger.info("{}dockerDaemon={}", LOADING, dockerDaemon);
        logger.info("{}composeDocument={}", LOADING, composeDocument);
        logger.info("{}containerStateDirectoryPath={}", LOADING, containerStateDirectoryPath);

        initializeOptions();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void initializeOptions() {
        logger.trace("{}Initializing options", LOADING);
        List<String> list = new ArrayList<>();
        list.add("--no-ansi");
        list.add("--project-name " + composeDocument.projectName());
        list.add("--file " + composeDocument.filePath());
        list.add("--project-directory " + composeDocument.projectDirectory());
        this.options = list.stream().collect(Collectors.joining(" "));
        logger.info("{}options={}", LOADING, options);
    }

    private void createNetwork() {
        logger.debug("Creating default network");
        BashScript script = createScript();
        String networkName = composeDocument.projectName() + "_default";
        script.command("docker network create --attachable " + networkName);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to create network", e);
        }
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
        logger.debug("Creating containers: {}; Scale: {}", serviceName, serviceScale);
        final BashScript script = createScript();
        final String upOptions = "--no-start --scale " + serviceName + "=" + serviceScale;
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + serviceName);
        script.forEachOutput(forEachOutput);
        script.forEachError(message -> {
            String msg = message.trim();
            if (msg.startsWith("Pulling " + serviceName + " ...") || msg.equals("Image for service " + serviceName
                    + " was built because it did not already exist. To rebuild this image you must use `docker-compose build` or `docker-compose up --build`.")
                    || matches(msg, "Creating " + projectName + "_" + serviceName + "_\\d+ \\.\\.\\. done")) {
                /* ignore */
            } else if (msg.equals("Building " + serviceName)
                    || matches(msg, "Pulling " + serviceName + " \\(.+\\)\\.\\.\\.")
                    || matches(msg, "Creating " + projectName + "_" + serviceName + "_\\d+ \\.\\.\\.")) {
                forEachOutput.accept(message);
            } else {
                forEachError.accept(message);
            }
        });
        try {
            script.run();
            return true;
        } catch (BashScriptFailedException e) {
            return false;
        }
    }

    private static boolean matches(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    @Override
    public boolean runContainer(String containerName, Consumer<String> forEachOutput, Consumer<String> forEachError) {
        logger.debug("Starting container: {}", containerName);
        final BashScript script = createScript();
        script.command("docker container start --attach " + containerName);
        script.forEachOutput(forEachOutput);
        script.forEachError(forEachError);
        synchronized (runningContainers) {
            runningContainers.add(containerName);
        }
        try {
            script.run();
            return true;
        } catch (BashScriptFailedException e) {
            return false;
        }
    }

    @Override
    public DockerContainerState getContainerState(String containerName) {
        logger.debug("Getting container state: {}", containerName);
        final List<String> scriptOutput = new ArrayList<>();
        final BashScript script = createScript();
        String f = containerStateDirectoryPath + "/" + containerName;
        script.command("docker container inspect " + containerName + " > " + f);
        script.command("cat " + f + " | jq --compact-output '.[] | .State'");
        script.command("STATUS=$(cat " + f + " | jq -r '.[] | .State.Status')");
        script.command("cat " + f + " > " + f + ".$STATUS");
        script.forEachOutput(scriptOutput::add);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to get container state", e);
        }
        final String json = scriptOutput.stream().collect(Collectors.joining());
        logger.debug("Container state JSON ({}): {}", containerName, json);
        DockerContainerState containerState = parseContainerStateJson(json);
        if (containerState.exited()) {
            synchronized (runningContainers) {
                runningContainers.remove(containerName);
            }
        }
        return containerState;
    }

    private DockerContainerState parseContainerStateJson(String json) {
        try {
            return new ObjectMapper().readValue(json, DockerContainerState.class);
        } catch (JsonProcessingException e) {
            throw new DockerAdapterException("Unable to parse container state JSON", e);
        }
    }

    @Override
    public boolean stopContainer(String containerName) {
        logger.debug("Stopping container: {}", containerName);
        BashScript script = createScript();
        script.command("docker container stop " + containerName);
        try {
            script.run();
            return true;
        } catch (BashScriptFailedException e) {
            return false;
        }
    }

    private BashScript createScript() {
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + dockerDaemon.socketAddress());
        return script;
    }

    private void killContainer(String containerName) {
        logger.error("Killing container: {}", containerName);
        BashScript script = createScript();
        script.command("docker container kill " + containerName);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            /* ignore */
        }
    }

    private void shutdown() {
        synchronized (this) {
            Set<String> containersToKill = new HashSet<>(runningContainers);
            containersToKill.forEach(containerName -> new Thread(() -> {
                killContainer(containerName);
                getContainerState(containerName);
            }).start());
        }
    }

    @Override
    public void disconnect() {
        dockerDaemon.disconnect();
    }
}