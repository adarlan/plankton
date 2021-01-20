package me.adarlan.plankton.docker;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;

@ToString(of = { "id", "dockerHostSocketAddress", "socketAddress" })
public class DockerSandbox implements DockerDaemon {

    private final String dockerHostSocketAddress;
    private final String id;

    private final boolean runningFromHost;
    private final String runningFromContainerId;

    private final String containerName;
    private final String networkName;

    private final String workspaceDirectoryPath;
    private final String underlyingWorkspaceDirectoryPath;

    private final String socketAddress;
    private final String socketIp;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String LOADING = "Loading " + DockerSandbox.class.getSimpleName() + " ... ";

    public DockerSandbox(DockerSandboxConfiguration configuration) {

        logger.info(LOADING);

        this.runningFromHost = configuration.runningFromHost();
        this.runningFromContainerId = configuration.runningFromContainerId();
        this.dockerHostSocketAddress = configuration.dockerHostSocketAddress();
        this.id = configuration.id();
        this.containerName = id + "_sandbox";
        if (runningFromHost) {
            this.networkName = null;
            this.socketIp = "127.0.0.1";
        } else {
            this.networkName = id + "_sandbox";
            this.socketIp = containerName;
        }
        this.socketAddress = "tcp://" + socketIp + ":2375";
        this.workspaceDirectoryPath = configuration.workspaceDirectoryPath();
        this.underlyingWorkspaceDirectoryPath = configuration.underlyingWorkspaceDirectoryPath();

        logger.info("{}id={}", LOADING, id);
        logger.info("{}dockerHostSocketAddress={}", LOADING, dockerHostSocketAddress);
        logger.info("{}containerName={}", LOADING, containerName);
        logger.info("{}socketAddress={}", LOADING, socketAddress);
        logger.info("{}workspaceDirectoryPath={}", LOADING, workspaceDirectoryPath);
        logger.info("{}dockerHostWorkspaceDirectoryPath={}", LOADING, underlyingWorkspaceDirectoryPath);

        if (!runningFromHost) {
            createBridgeNetwork();
            connectBridgeNetwork();
            inspectBridgeNetwork();
        }
        createContainer();
        startContainer();
        try {
            waitUntilContainerIsReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted when waiting for sandbox container to be ready", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void createBridgeNetwork() {
        logger.info("{}Creating sandbox bridge network", LOADING);
        BashScript script = createDockerHostScript("create_sandbox_network");
        script.command("docker network create --driver bridge --attachable " + networkName);
        script.runSuccessfully();
    }

    private void connectBridgeNetwork() {
        logger.info("{}Connecting sandbox bridge network", LOADING);
        BashScript script = createDockerHostScript("connect_sandbox_network");
        script.command("docker network connect " + networkName + " " + runningFromContainerId);
        script.runSuccessfully();
    }

    private void inspectBridgeNetwork() {
        logger.info("{}Inspecting sandbox bridge network", LOADING);
        BashScript script = createDockerHostScript("inspect_sandbox_network");
        script.command("docker network inspect " + networkName);
        script.runSuccessfully();
    }

    private void createContainer() {
        logger.info("{}Creating sandbox container", LOADING);

        List<String> containerOptionList = new ArrayList<>();
        containerOptionList.add("--runtime sysbox-runc");
        containerOptionList.add("--name " + containerName);
        containerOptionList.add("--tty");
        containerOptionList.add("--rm");
        containerOptionList.add("-v " + underlyingWorkspaceDirectoryPath + ":" + workspaceDirectoryPath);
        if (runningFromHost) {
            containerOptionList.add("-p 2375:2375");
        } else {
            containerOptionList.add("--network " + networkName);
        }

        List<String> sandboxOptionList = new ArrayList<>();
        sandboxOptionList.add("-H tcp://0.0.0.0:2375");
        sandboxOptionList.add("-H unix:///var/run/docker.sock");

        String containerOptions = containerOptionList.stream().collect(Collectors.joining(" "));
        String sandboxOptions = sandboxOptionList.stream().collect(Collectors.joining(" "));

        BashScript script = createDockerHostScript("create_sandbox_container");
        script.command("docker container create " + containerOptions + " adarlan/plankton:sandbox " + sandboxOptions);
        script.run();
        if (script.getExitCode() != 0) {
            throw new DockerSandboxException(
                    "Unable to create sandbox container; Script exited with code: " + script.getExitCode());
        }
    }

    private void startContainer() {
        new Thread(() -> {
            logger.info("{}Starting sandbox container", LOADING);
            BashScript script = createDockerHostScript("start_sandbox_container");
            script.command("docker container start --attach " + containerName);
            script.forEachOutput(logger::info); // TODO add prefix
            script.forEachError(logger::error); // TODO add prefix
            script.run();
            if (script.getExitCode() != 0) {
                throw new DockerSandboxException(
                        "Sandbox container exited with a non-zero code: " + script.getExitCode());
            }
        }).start();
    }

    private void waitUntilContainerIsReady() throws InterruptedException {
        boolean socketReady = false;
        boolean daemonRunning = false;
        while (!socketReady || !daemonRunning) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            logger.info("{}Waiting for sandbox container to be ready", LOADING);
            if (!socketReady)
                socketReady = socketIsReady();
            if (!daemonRunning)
                daemonRunning = daemonIsRunning();
            Thread.sleep(1000);
        }
        logger.info("{}Sandbox container is ready", LOADING);
    }

    private boolean socketIsReady() {
        try (Socket s = new Socket(socketIp, 2375)) {
            logger.info("{}Sandbox socket is ready", LOADING);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean daemonIsRunning() {
        BashScript script = new BashScript("check_docker_daemon");
        script.env("DOCKER_HOST=" + socketAddress);
        script.command("docker ps");
        script.forEachOutputAndError(msg -> {
            /* do nothing */
        });
        script.run();
        if (script.getExitCode() == 0) {
            logger.info("{}Sandbox daemon is running", LOADING);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getSocketAddress() {
        return socketAddress;
    }

    public void shutdown() {
        logger.info("Shutdown {}", this);
        BashScript script = createDockerHostScript("stop_sandbox_container");
        script.command("docker stop " + containerName);
        if (!runningFromHost) {
            script.command("docker network disconnect " + networkName + " " + runningFromContainerId);
            script.command("docker network rm " + networkName);
        }
        script.runSuccessfully();
    }

    private BashScript createDockerHostScript(String name) {
        BashScript script = new BashScript(name);
        script.env("DOCKER_HOST=" + dockerHostSocketAddress);
        return script;
    }
}
