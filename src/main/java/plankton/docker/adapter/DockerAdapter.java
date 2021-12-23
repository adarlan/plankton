package plankton.docker.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.compose.ComposeService;
import plankton.compose.serviceprops.Build;
import plankton.docker.client.DockerClient;
import plankton.docker.daemon.DockerDaemon;
import plankton.pipeline.ContainerConfiguration;
import plankton.pipeline.ContainerRuntimeAdapter;
import plankton.util.BashScript;
import plankton.util.BashScriptFailedException;

public class DockerAdapter implements ContainerRuntimeAdapter {

    private final String workspacePathFromRunnerPerspective;
    private final String workspacePathFromAdapterPerspective;

    private final DockerDaemon daemon;
    private final String namespace;
    private final String networkName;

    private final DockerClient dockerClient;

    private static final Logger logger = LoggerFactory.getLogger(DockerAdapter.class);
    private final String prefix = DockerAdapter.class.getSimpleName() + " ... ";

    public DockerAdapter(DockerAdapterConfiguration configuration) {

        this.workspacePathFromRunnerPerspective = configuration.workspacePathFromRunnerPerspective();
        this.workspacePathFromAdapterPerspective = configuration.workspacePathFromAdapterPerspective();

        this.daemon = configuration.dockerDaemon();
        this.namespace = configuration.namespace();
        this.networkName = namespace + "_network";

        this.dockerClient = new DockerClient(daemon);

        logger.debug("workspacePathFromRunnerPerspective={}", workspacePathFromRunnerPerspective);
        logger.debug("workspacePathFromAdapterPerspective={}", workspacePathFromAdapterPerspective);

        logger.debug("daemon={}", daemon);
        logger.debug("namespace={}", namespace);
        logger.debug("networkName={}", networkName);

        createNetwork();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void createNetwork() {
        dockerClient.networkCreator().createAttachableNetwork(networkName);
    }

    private final Set<String> buildImageSet = new HashSet<>();
    private final Set<String> createContainerSet = new HashSet<>();

    private final Set<String> startedContainers = new HashSet<>();
    private final Set<String> runningContainers = new HashSet<>();
    private final Set<String> exitedContainers = new HashSet<>();

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
    public void pullImage(ContainerConfiguration config) {
        ComposeService service = config.getService();
        String imageTag = service.image().orElseThrow();
        if (!imageExists(imageTag)) {
            dockerClient.imagePuller()
                    .forEachOutput(config.getForEachOutput())
                    .forEachError(config.getForEachError())
                    .pullImage(imageTag);
        }
    }

    private boolean imageExists(String imageTag) {
        return dockerClient.imageExists(imageTag);
    }

    @Override
    public void buildImage(ContainerConfiguration config) {
        ComposeService service = config.getService();
        String imageTag;
        Optional<String> optionalImageTag = service.image();
        if (optionalImageTag.isPresent())
            imageTag = optionalImageTag.get();
        else
            imageTag = namespace + "_" + service.name();
        setBuildImage(imageTag);
        logger.debug("{}Building image for: {}", prefix, service);
        Build build = service.build().orElseThrow();
        String context = build.getContext();
        String dockerfile = build.getDockerfile();
        dockerClient.imageBuilder()
                .context(context)
                .option("-t " + imageTag)
                .option(dockerfile == null ? "" : " -f " + dockerfile)
                .forEachOutput(config.getForEachOutput())
                .forEachError(config.getForEachError())
                .buildImage();

        // TODO get more build options from service
    }

    @Override
    public void createContainer(ContainerConfiguration config) {
        ComposeService service = config.getService();
        logger.debug("{}Creating containers for: {}", prefix, service);

        String containerName = namespace + "_" + service.name();
        setCreateContainer(containerName);

        DockerClient.ContainerCreator containerCreator = dockerClient.containerCreator();

        containerCreator.option("--name " + containerName);

        containerCreator.option("--network " + networkName);
        containerCreator.option("--hostname " + service.name());

        service.environment().forEach(e -> containerCreator.option("--env \"" + e + "\""));
        // TODO what if it contains: "

        service.envFile().forEach(f -> containerCreator.option("--env-file " + f));
        service.expose().forEach(p -> containerCreator.option("--expose " + p));
        service.groupAdd().forEach(g -> containerCreator.option("--group-add " + g));
        service.user().ifPresent(u -> containerCreator.option("--user " + u));
        service.workingDir().ifPresent(w -> containerCreator.option("--workdir " + w));

        if (service.entrypointIsReseted())
            containerCreator.option("--entrypoint \"\"");
        else if (!service.entrypoint().isEmpty()) {

            runBashScript("mkdir -p " + workspacePathFromRunnerPerspective + "/.plankton");
            String entrypointFileName = namespace + "_" + service.name() + ".entrypoint.sh";
            String entrypointFilePathFromPlanktonPerspective = workspacePathFromRunnerPerspective + "/.plankton/"
                    + entrypointFileName;
            String entrypointFilePathFromAdapterPerspective = workspacePathFromAdapterPerspective + "/.plankton/"
                    + entrypointFileName;
            String entrypointFilePathFromJobContainerPerspective = "/docker-entrypoint.sh";

            createEntrypointFile(service, entrypointFilePathFromPlanktonPerspective);
            containerCreator.option("-v " + entrypointFilePathFromAdapterPerspective + ":"
                    + entrypointFilePathFromJobContainerPerspective);
            containerCreator.option("--entrypoint " + entrypointFilePathFromJobContainerPerspective);
        }

        service.healthcheck().ifPresent(h -> {
            // TODO
            // --health-cmd string
            // --health-interval duration
            // --health-retries int
            // --health-start-period duration
            // --health-timeout duration
        });

        service.volumes().forEach(v -> containerCreator.option("--volume " + v));

        containerCreator.image(service.image().orElseThrow());
        containerCreator.args(service.command().stream().collect(Collectors.joining(" ")));

        containerCreator.forEachOutput(config.getForEachOutput());
        containerCreator.forEachError(config.getForEachError());
        containerCreator.createContainer();
    }

    private synchronized void createEntrypointFile(ComposeService service,
            String entrypointFilePathFromPlanktonPerspective) {
        List<String> entrypointList = service.entrypoint();
        List<String> commands = new ArrayList<>();
        commands.add("#!/bin/sh");
        entrypointList.forEach(commands::add);
        try {
            FileSystemUtils.writeFile(entrypointFilePathFromPlanktonPerspective, commands);
        } catch (IOException e) {
            throw new DockerAdapterException("Unable to write entrypoint file", e);
        }
        runBashScript("chmod +x " + entrypointFilePathFromPlanktonPerspective);
    }

    private void runBashScript(String command) {
        BashScript script = new BashScript();
        script.command(command);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerAdapterException("Unable to run bash script", e);
        }
    }

    @Override
    public int startContainerAndGetExitCode(ContainerConfiguration config) {
        ComposeService service = config.getService();
        String containerName = namespace + "_" + service.name();
        setStartedContainer(containerName);
        DockerClient.ContainerStarter containerStarter = dockerClient.containerStarter()
                .forEachOutput(config.getForEachOutput())
                .forEachError(config.getForEachError());
        int exitCode = containerStarter.startAndGetExitCode(containerName);
        setExitedContainers(containerName);
        return exitCode;
    }

    @Override
    public void stopContainer(ContainerConfiguration config) {
        ComposeService service = config.getService();
        String containerName = namespace + "_" + service.name();
        dockerClient.containerStopper()
                .forEachOutput(config.getForEachOutput())
                .forEachError(config.getForEachError())
                .stopContainer(containerName);
    }

    private void killContainer(String containerName) {
        final String logPlaceholder = "Killing container '" + containerName + "' ... {}";
        dockerClient.containerKiller()
                .allowFailure()
                .forEachOutput(msg -> logger.info(logPlaceholder, msg))
                .forEachError(msg -> logger.error(logPlaceholder, msg))
                .killContainer(containerName);
    }

    private void shutdown() {
        synchronized (this) {
            Set<String> containersToKill = new HashSet<>(runningContainers);
            containersToKill.forEach(containerName -> new Thread(() -> killContainer(containerName)).start());
        }
    }
}