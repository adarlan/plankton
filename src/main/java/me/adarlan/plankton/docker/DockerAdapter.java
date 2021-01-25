package me.adarlan.plankton.docker;

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

import lombok.ToString;

import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.core.ContainerRuntimeAdapter;

@ToString(of = { "dockerDaemon", "composeDocument" })
public class DockerAdapter implements ContainerRuntimeAdapter {

    private final DockerDaemon dockerDaemon;
    private final ComposeDocument composeDocument;

    private final String projectName;
    private final String networkName;

    private final Map<ComposeService, Thread> threadsForCreateContainers = new HashMap<>();
    private final Set<String> runningContainers = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String LOADING = "Loading DockerAdapter ... ";
    private static final String MSG_PLACEHOLDER = " -> {}";

    public DockerAdapter(DockerAdapterConfiguration configuration) {

        logger.info(LOADING);

        this.dockerDaemon = configuration.dockerDaemon();
        this.composeDocument = configuration.composeDocument();
        this.projectName = composeDocument.projectName();
        this.networkName = projectName + "_network";

        logger.info("{}dockerDaemon={}", LOADING, dockerDaemon);
        logger.info("{}composeDocument={}", LOADING, composeDocument);

        createNetwork();
        startThreadsForCreateContainers();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void createNetwork() {
        String command = "docker network create --attachable " + networkName;
        String logPlaceholder = LOADING + "Creating network: " + networkName + MSG_PLACEHOLDER;
        runCommand(command, logPlaceholder);
    }

    private void startThreadsForCreateContainers() {
        logger.info("{}Starting threads for create containers", LOADING);
        composeDocument.services().forEach(service -> {
            Thread thread = getThreadForCreateContainersOf(service);
            thread.start();
        });
    }

    private Thread getThreadForCreateContainersOf(ComposeService s) {
        return threadsForCreateContainers.computeIfAbsent(s, service -> {
            Thread thread = new Thread(() -> {
                buildOrPullImage(service);
                for (int i = 0; i < service.scale(); i++) {
                    createContainer(service, i);
                }
            });
            thread.setUncaughtExceptionHandler((t, e) -> {
                throw new DockerAdapterException("Unable to create containers for: " + service, e);
            });
            return thread;
        });
    }

    private void buildOrPullImage(ComposeService service) {
        Optional<ComposeService.Build> build = service.build();
        if (build.isPresent()) {
            buildImage(service.imageTag(), build.get().context, build.get().dockerfile);
        } else {
            pullImage(service.imageTag());
        }
    }

    private void buildImage(String imageTag, String context, String dockerfile) {
        String command = "docker image build -t " + imageTag + " -f " + dockerfile + " " + context;
        String logPlaceholder = "Building image: " + imageTag + MSG_PLACEHOLDER;
        runCommand(command, logPlaceholder);
    }

    private void pullImage(String imageTag) {
        // TODO credential_spec
        String command = "docker pull " + imageTag;
        String logPlaceholder = "Pulling image: " + imageTag + MSG_PLACEHOLDER;
        runCommand(command, logPlaceholder);
    }

    private void runCommand(String command, String logPlaceholder) {
        int exitCode = runCommandAndGetExitCode(command, logPlaceholder);
        if (exitCode != 0) {
            throw new DockerAdapterException(
                    "Docker command exited a non-zero code: " + exitCode + "; Command: " + command);
        }
    }

    private int runCommandAndGetExitCode(String command, String logPlaceholder) {
        logger.info(logPlaceholder, command);
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + dockerDaemon.socketAddress());
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

    @Override
    public void createContainers(ComposeService service) {
        Thread thread = getThreadForCreateContainersOf(service);
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerAdapterException("Interrupted when creating containers of: " + service, e);
        }
    }

    private void createContainer(ComposeService service, int containerIndex) {

        String containerName = composeDocument.containerNameOf(service, containerIndex);

        // TODO avoid shell script injection
        // validating properties with regex?
        // using the docker rest api?

        List<String> command = new ArrayList<>();

        command.add("docker container create");
        command.add("--name " + containerName);

        command.add("--network " + networkName);
        // TODO --hostname
        // if (scale = 1) --hostname ${service.name}
        // if (scale > 1) --hostname ${service.name}_${instance.index}

        service.environment().forEach(e -> command.add("--env " + e));
        service.envFile().forEach(f -> command.add("--env-file " + f));
        service.expose().forEach(p -> command.add("--expose " + p));
        service.groupAdd().forEach(g -> command.add("--group-add " + g));
        service.user().ifPresent(u -> command.add("--user " + u));
        service.workingDir().ifPresent(w -> command.add("--workdir " + w));

        List<String> entrypoint = service.entrypoint();
        if (!entrypoint.isEmpty()) {
            if (entrypoint.size() == 1 && entrypoint.get(0).trim().equals(""))
                command.add("--entrypoint \"\"");
            else
                command.add("--entrypoint " + entrypoint.stream().collect(Collectors.joining(" ")));
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
        // TODO --volumes-from

        command.add(service.imageTag());
        command.add(service.command().stream().collect(Collectors.joining(" ")));

        String dockerCommand = command.stream().collect(Collectors.joining(" "));

        String logPlaceholder = "Creating container: " + containerName + MSG_PLACEHOLDER;
        runCommand(dockerCommand, logPlaceholder);
    }

    @Override
    public int runContainerAndGetExitCode(ComposeService service, int containerIndex) {
        String containerName = composeDocument.containerNameOf(service, containerIndex);
        String command = "docker container start --attach " + containerName;
        synchronized (runningContainers) {
            runningContainers.add(containerName);
        }
        String logPlaceholder = "Running container: " + containerName + MSG_PLACEHOLDER;
        int exitCode = runCommandAndGetExitCode(command, logPlaceholder);
        synchronized (runningContainers) {
            runningContainers.remove(containerName);
        }
        return exitCode;
    }

    @Override
    public void stopContainer(ComposeService service, int containerIndex) {
        String containerName = composeDocument.containerNameOf(service, containerIndex);
        String command = "docker container stop " + containerName;
        String logPlaceholder = "Stopping container: " + containerName + MSG_PLACEHOLDER;
        runCommand(command, logPlaceholder);
    }

    private void killContainer(String containerName) {
        String command = "docker container kill " + containerName;
        String logPlaceholder = "Killing container: " + containerName + MSG_PLACEHOLDER;
        int exitCode = runCommandAndGetExitCode(command, logPlaceholder);
        if (exitCode != 0) {
            logger.warn("Unable to kill container: {}", containerName);
        }
    }

    private void shutdown() {
        synchronized (this) {
            Set<String> containersToKill = new HashSet<>(runningContainers);
            containersToKill.forEach(containerName -> new Thread(() -> killContainer(containerName)).start());
        }
    }
}