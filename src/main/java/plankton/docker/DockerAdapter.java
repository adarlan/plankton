package plankton.docker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeService;
import plankton.core.ContainerRuntimeAdapter;
import plankton.util.Colors;
import plankton.util.FileSystemUtils;
import plankton.util.LogUtils;

public class DockerAdapter implements ContainerRuntimeAdapter {

    private final ComposeDocument composeDocument;
    private final String projectDirectoryPath;
    private final String projectDirectoryTargetPath;

    private final DockerDaemon daemon;
    private final String namespace;
    private final String networkName;

    private static final Logger logger = LoggerFactory.getLogger(DockerAdapter.class);
    private final String prefix = DockerAdapter.class.getSimpleName() + " ... ";

    public DockerAdapter(DockerAdapterConfiguration configuration) {

        this.composeDocument = configuration.composeDocument();
        this.projectDirectoryPath = configuration.projectDirectoryPath();
        this.projectDirectoryTargetPath = configuration.projectDirectoryTargetPath();

        this.daemon = configuration.dockerDaemon();
        this.namespace = configuration.namespace();
        this.networkName = namespace + "_network";

        logger.debug("composeDocument={}", composeDocument);
        logger.debug("projectDirectoryPath={}", projectDirectoryPath);
        logger.debug("projectDirectoryTargetPath={}", projectDirectoryTargetPath);

        logger.debug("daemon={}", daemon);
        logger.debug("namespace={}", namespace);
        logger.debug("networkName={}", networkName);

        createNetwork();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void createNetwork() {
        logger.debug("{}Creating network: {}", prefix, networkName);
        BashScript script = createBashScript();
        script.command("docker network create --attachable " + networkName);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to create network: " + networkName, e);
        }
    }

    // private final Set<String> pulledImages = new HashSet<>();
    private final Set<String> buildImageSet = new HashSet<>();
    private final Set<String> createContainerSet = new HashSet<>();

    private final Set<String> startedContainers = new HashSet<>();
    private final Set<String> runningContainers = new HashSet<>();
    private final Set<String> exitedContainers = new HashSet<>();

    // private synchronized void setPulledImage(String imageTag) {
    // if (pulledImages.contains(imageTag))
    // throw new DockerAdapterException("Image has already been pulled: " +
    // imageTag);
    // pulledImages.add(imageTag);
    // }

    private synchronized void setBuildImage(String imageTag) {
        if (buildImageSet.contains(imageTag))
            throw new DockerAdapterException("Image has already been built: " + imageTag);
        buildImageSet.add(imageTag);
    }

    private synchronized void setCreateContainer(String containerName) {
        if (createContainerSet.contains(containerName))
            throw new DockerAdapterException("Container has already been created: " + containerName);
        createContainerSet.add(containerName);
    }

    private synchronized void setStartedContainer(String containerName) {
        if (startedContainers.contains(containerName))
            throw new DockerAdapterException("Container has already been started: " + containerName);
        startedContainers.add(containerName);
        runningContainers.add(containerName);
    }

    private synchronized void setExitedContainers(String containerName) {
        if (exitedContainers.contains(containerName))
            throw new DockerAdapterException("Container has already been exited: " + containerName);
        exitedContainers.add(containerName);
        runningContainers.remove(containerName);
    }

    @Override
    public void pullImage(ComposeService service) {
        String imageTag = service.image()
                .orElseThrow(() -> new DockerAdapterException("Missing 'image' of: " + service.name()));
        if (!imageExists(imageTag)) {
            logger.debug("{}Pulling image for: {}", prefix, service);
            // TODO credential_spec
            String command = "docker pull " + imageTag;
            runCommand(command, service);
        }
    }

    private boolean imageExists(String imageTag) {
        logger.debug("{}Checking if image exists locally: {}", prefix, imageTag);
        List<String> list = new ArrayList<>();
        BashScript script = createBashScript();
        script.command("docker images -q " + imageTag);
        script.forEachOutput(list::add);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to check if image exists: " + imageTag, e);
        }
        if (list.isEmpty()) {
            logger.debug("{}Image DON'T exists locally: {}", prefix, imageTag);
            return false;
        } else {
            logger.debug("{}Image exists locally: {} ({})", prefix, imageTag, list);
            return true;
        }
    }

    @Override
    public void buildImage(ComposeService service) {
        String imageTag;
        Optional<String> optionalImageTag = service.image();
        if (optionalImageTag.isPresent())
            imageTag = optionalImageTag.get();
        else
            imageTag = namespace + "_" + service.name();
        setBuildImage(imageTag);
        logger.debug("{}Building image for: {}", prefix, service);
        ComposeService.Build build = service.build()
                .orElseThrow(() -> new DockerAdapterException("Missing 'build' of: " + service.name()));
        String context = build.context;
        String dockerfile = build.dockerfile;
        // TODO get more build options from service
        String command = "docker image build -t " + imageTag + (dockerfile == null ? "" : " -f " + dockerfile) + " "
                + context;
        runCommand(command, service);
    }

    @Override
    public void createContainers(ComposeService service) {
        logger.debug("{}Creating containers for: {}", prefix, service);
        for (int i = 0; i < service.scale(); i++) {
            createContainer(service, i);
        }
    }

    private void createContainer(ComposeService service, int containerIndex) {

        logger.debug("{}Creating container for: {}[{}]", prefix, service, containerIndex);

        String containerName = containerNameOf(service, containerIndex);
        setCreateContainer(containerName);

        // TODO avoid shell script injection
        // validating properties with regex?
        // using the docker rest api?

        List<String> command = new ArrayList<>();

        command.add("docker container create");
        command.add("--name " + containerName);

        command.add("--network " + networkName);
        if (service.scale() == 1)
            command.add("--hostname " + service.name());
        else
            command.add("--hostname " + service.name() + "_" + containerIndex);

        // TODO wrap each option value using quotes
        // or use the TCP API

        service.environment().forEach(e -> command.add("--env \"" + e + "\"")); // TODO what if it contains this: "
        service.envFile().forEach(f -> command.add("--env-file " + f));
        service.expose().forEach(p -> command.add("--expose " + p));
        service.groupAdd().forEach(g -> command.add("--group-add " + g));
        service.user().ifPresent(u -> command.add("--user " + u));
        service.workingDir().ifPresent(w -> command.add("--workdir " + w));

        if (service.entrypointIsReseted())
            command.add("--entrypoint \"\"");
        else if (!service.entrypoint().isEmpty()) {
            command.add("-v " + getEntrypointFileTargetPath(service) + ":/docker-entrypoint.sh");
            command.add("--entrypoint /docker-entrypoint.sh");
        }

        service.healthcheck().ifPresent(h -> {
            // TODO
            // --health-cmd string
            // --health-interval duration
            // --health-retries int
            // --health-start-period duration
            // --health-timeout duration
        });

        service.volumes().forEach(v -> command.add("--volume " + v));

        command.add(service.image().get());
        command.add(service.command().stream().collect(Collectors.joining(" ")));

        String dockerCommand = command.stream().collect(Collectors.joining(" "));
        logger.debug("{}Creating container for: {}[{}] -> Command: {}", prefix, service, containerIndex, dockerCommand);
        // runCommand(dockerCommand, service, "Creating container: " + containerName);
        runCommand(dockerCommand, service);
    }

    private final Map<ComposeService, String> entrypointFileTargetPaths = new HashMap<>();

    private synchronized String getEntrypointFileTargetPath(ComposeService service) {
        return entrypointFileTargetPaths.computeIfAbsent(service, s -> {

            List<String> entrypointList = s.entrypoint();

            String directoryName = ".plankton";
            String fileName = namespace + "_" + service.name() + ".entrypoint.sh";

            String directoryPath = projectDirectoryPath + "/" + directoryName;
            String filePath = directoryPath + "/" + fileName;

            String directoryTargetPath = projectDirectoryTargetPath + "/" + directoryName;
            String fileTargetPath = directoryTargetPath + "/" + fileName;

            runBashScript("mkdir -p " + directoryPath);

            List<String> commands = new ArrayList<>();
            commands.add("#!/bin/sh");
            commands.add("set -e");

            entrypointList.forEach(commands::add);
            // entrypointList.forEach(line -> {
            // commands.add("printf \"\\033[1;37m+ \\033[0m\" || true");
            // for (int i = 0; i < line.length(); i++) {
            // commands.add("printf \"\\033[1;37m" + line.charAt(i) + "\\033[0m\" || true");
            // }
            // commands.add("printf \"\\n\" || true");
            // commands.add(line);
            // });

            FileSystemUtils.writeFile(filePath, commands);

            runBashScript("chmod +x " + filePath);

            return fileTargetPath;
        });
    }

    private void runBashScript(String command) {
        try {
            BashScript.run(command);
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to run bash script", e);
        }
    }

    @Override
    public int runContainerAndGetExitCode(ComposeService service, int containerIndex) {
        String containerName = containerNameOf(service, containerIndex);
        if (!createContainerSet.contains(containerName)) {
            createContainer(service, containerIndex);
        }
        setStartedContainer(containerName);
        String command = "docker container start --attach " + containerName;
        String logPlaceholder = logPrefixOf(service, containerIndex) + MSG;
        BashScript script = createBashScript();
        script.command(command);
        script.forEachOutput(msg -> logger.info(logPlaceholder, msg));
        script.forEachError(msg -> logger.error(logPlaceholder, msg));
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            /* ignore */
        }
        int exitCode = script.exitCode();
        setExitedContainers(containerName);
        return exitCode;
    }

    @Override
    public void stopContainer(ComposeService service, int containerIndex) {
        String containerName = containerNameOf(service, containerIndex);
        String command = "docker container stop " + containerName;
        runCommand(command, service);
    }

    private String containerNameOf(ComposeService service, int containerIndex) {
        return namespace + "_" + service.name() + (service.scale() > 1 ? ("_" + containerIndex) : "");
    }

    private void killContainer(String containerName) {
        String command = "docker container kill " + containerName;
        int exitCode = runCommandAndGetExitCode(command);
        if (exitCode != 0) {
            logger.warn("{} ... Unable to kill container: {}", this, containerName);
        }
    }

    private void shutdown() {
        synchronized (this) {
            Set<String> containersToKill = new HashSet<>(runningContainers);
            containersToKill.forEach(containerName -> new Thread(() -> killContainer(containerName)).start());
        }
    }

    private static final String MSG = "{}" + Colors.ANSI_RESET;

    private String logPrefixOf(ComposeService service) {
        return LogUtils.prefixOf(service.name());
    }

    private String logPrefixOf(ComposeService service, int containerIndex) {
        if (service.scale() == 1)
            return LogUtils.prefixOf(service.name());
        else
            return LogUtils.prefixOf(service.name(), "[" + containerIndex + "]");
    }

    private void runCommand(String command, ComposeService service) {
        String logPlaceholder = logPrefixOf(service) + MSG;
        runScript(command, logPlaceholder);
    }

    // private void runCommand(String command, ComposeService service, String
    // message) {
    // String logPlaceholder = logPrefixOf(service) + message + ARROW_MSG;
    // runScript(command, logPlaceholder);
    // }

    // private int runCommandAndGetExitCode(String command, String message) {
    private int runCommandAndGetExitCode(String command) {
        // String logPlaceholder = message + ARROW_MSG;
        return runScriptAndGetExitCode(command, MSG);
    }

    // private int runCommandAndGetExitCode(String command, ComposeService service,
    // int containerIndex) {
    // String logPlaceholder = logPrefixOf(service, containerIndex) + MSG;
    // return runScriptAndGetExitCode(command, logPlaceholder);
    // }

    private void runScript(String command, String logPlaceholder) {
        int exitCode = runScriptAndGetExitCode(command, logPlaceholder);
        if (exitCode != 0) {
            throw new DockerAdapterException("Command exited non-zero code: " + exitCode + " -> Command: " + command);
        }
    }

    private int runScriptAndGetExitCode(String command, String logPlaceholder) {
        BashScript script = createBashScript();
        script.command(command);
        script.forEachOutput(msg -> logger.info(logPlaceholder, msg));
        script.forEachError(msg -> logger.error(logPlaceholder, msg));
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            /* ignore */
        }
        return script.exitCode();
    }

    private BashScript createBashScript() {
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + daemon.socketAddress());
        return script;
    }
}