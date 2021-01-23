package me.adarlan.plankton.docker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;
import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeService;
import me.adarlan.plankton.util.Utils;

@ToString(of = { "dockerDaemon", "composeDocument" })
public class DockerAdapter implements ComposeAdapter {

    private final DockerDaemon dockerDaemon;
    private final ComposeDocument composeDocument;

    private final String projectName;
    private final String networkName;

    private final Set<String> runningContainers = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String LOADING = "Loading DockerAdapter ... ";
    private static final String PREFIX_MSG = "{}{}";

    public DockerAdapter(DockerAdapterConfiguration configuration) {

        logger.info(LOADING);

        this.dockerDaemon = configuration.dockerDaemon();
        this.composeDocument = configuration.composeDocument();
        this.projectName = composeDocument.projectName();
        this.networkName = projectName + "_network";

        logger.info("{}dockerDaemon={}", LOADING, dockerDaemon);
        logger.info("{}composeDocument={}", LOADING, composeDocument);

        createNetwork();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void createNetwork() {
        String description = "Creating network: " + networkName;
        String command = "docker network create --attachable " + networkName;
        runDockerCommand(command, description);
    }

    private void runDockerCommand(String command, String description) {
        int exitCode = runDockerCommandAndGetExitCode(command, description);
        if (exitCode != 0) {
            throw new DockerAdapterException(
                    "Docker command exited a non-zero code: " + exitCode + "; Command: " + command);
        }
    }

    private int runDockerCommandAndGetExitCode(String command, String description) {
        BashScript script = createScript();
        script.command(command);
        String prefix = description + " ... ";
        logger.info(prefix);
        script.forEachOutput(msg -> logger.info(PREFIX_MSG, prefix, msg));
        script.forEachError(msg -> logger.error(PREFIX_MSG, prefix, msg));
        try {
            script.run();
        } catch (BashScriptFailedException e) {
        }
        return script.exitCode();
    }

    private void buildImage(String image, String context, String dockerfile) {
        String command = "docker image build -t " + image + " -f " + dockerfile + " " + context;
        String description = "Building image: " + image;
        runDockerCommand(command, description);
    }

    private void pullImage(String imageTag) {
        // TODO service.credential_spec
        String description = "Pulling image: " + imageTag;
        String command = "docker pull " + imageTag;
        runDockerCommand(command, description);
    }

    private void createContainer(ComposeService service, String image, int containerIndex) {

        String containerName = composeDocument.containerNameOf(service, containerIndex);
        String description = "Creating container: " + containerName;

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
        service.entrypoint().ifPresent(e -> command.add("--entrypoint " + e));

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

        command.add(image);
        service.command().ifPresent(command::add);

        String dockerCommand = Utils.join(command, " ");
        runDockerCommand(dockerCommand, description);
    }

    @Override
    public void createContainers(ComposeService service) {
        String imageTag;
        Optional<ComposeService.Build> build = service.build();
        if (build.isPresent()) {
            imageTag = projectName + "_" + service.name();
            buildImage(imageTag, build.get().context, build.get().dockerfile);
        } else {
            imageTag = service.image()
                    .orElseThrow(() -> new DockerAdapterException("Missing " + service.name() + ".image"));
            pullImage(imageTag);
        }
        for (int index = 0; index < service.scale(); index++) {
            createContainer(service, imageTag, index);
        }
    }

    @Override
    public int runContainer(ComposeService service, int containerIndex) {
        String containerName = composeDocument.containerNameOf(service, containerIndex);
        String description = "Running container: " + containerName;
        String command = "docker container start --attach " + containerName;
        synchronized (runningContainers) {
            runningContainers.add(containerName);
        }
        int exitCode = runDockerCommandAndGetExitCode(command, description);
        synchronized (runningContainers) {
            runningContainers.remove(containerName);
        }
        return exitCode;
    }

    @Override
    public void stopContainer(ComposeService service, int containerIndex) {
        String containerName = composeDocument.containerNameOf(service, containerIndex);
        String description = "Stopping container: " + containerName;
        String command = "docker container stop " + containerName;
        runDockerCommand(command, description);
    }

    private BashScript createScript() {
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + dockerDaemon.socketAddress());
        return script;
    }

    private void killContainer(String containerName) {
        logger.info("Killing container: {}", containerName);
        BashScript script = createScript();
        script.command("docker container kill " + containerName);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            logger.error("Unable to kill container: {}", containerName, e);
        }
    }

    private void shutdown() {
        synchronized (this) {
            Set<String> containersToKill = new HashSet<>(runningContainers);
            containersToKill.forEach(containerName -> new Thread(() -> killContainer(containerName)).start());
        }
    }
}