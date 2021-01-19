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

    private boolean fromHost;
    private String runnerContainerId;

    private final String containerName;
    private final String networkName;

    private final String dockerHostWorkspaceDirectoryPath;
    private final String workspaceDirectoryPath;

    private final String socketAddress;
    private final String socketIp;

    private final Thread getReady;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DockerSandbox(DockerSandboxConfiguration configuration) {
        logger.info("Loading DockerSandbox");
        this.id = configuration.id();
        logger.info("id={}", id);
        this.dockerHostSocketAddress = configuration.dockerHostConfiguration().socketAddress();
        logger.info("dockerHostSocketAddress={}", dockerHostSocketAddress);
        initializeFromHostFlagAndRunnerContainerId();
        this.containerName = id + "_sandbox";
        logger.info("containerName={}", containerName);
        if (fromHost) {
            networkName = null;
            socketIp = "127.0.0.1";
        } else {
            networkName = id + "_sandbox";
            socketIp = containerName;
        }
        this.socketAddress = "tcp://" + socketIp + ":2375";
        logger.info("socketAddress={}", socketAddress);
        this.workspaceDirectoryPath = configuration.workspaceDirectoryPath();
        logger.info("workspaceDirectoryPath={}", workspaceDirectoryPath);
        this.dockerHostWorkspaceDirectoryPath = configuration.dockerHostConfiguration().workspaceDirectoryPath();
        logger.info("dockerHostWorkspaceDirectoryPath={}", dockerHostWorkspaceDirectoryPath);
        this.getReady = new Thread(this::getReady);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        getReady.start();
    }

    private void initializeFromHostFlagAndRunnerContainerId() {
        // TODO isso deveria ser parte de DockerHost... ou DockerRunnerConfiguration???
        logger.info("Initializing mode");
        List<String> scriptOutput = new ArrayList<>();
        BashScript script = new BashScript("get_sandbox_container_id");
        script.command("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        script.forEachOutput(scriptOutput::add);
        script.runSuccessfully();
        if (scriptOutput.isEmpty()) {
            this.fromHost = true;
            this.runnerContainerId = null;
            logger.info("Running Plankton directly from Docker host");
        } else {
            this.fromHost = false;
            this.runnerContainerId = scriptOutput.stream().collect(Collectors.joining());
            logger.info("Running Plankton from within a Docker container: {}", runnerContainerId);
        }
    }

    private void getReady() {
        if (!fromHost) {
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
    }

    private void createBridgeNetwork() {
        logger.info("Creating sandbox bridge network");
        BashScript script = createDockerHostScript("create_sandbox_network");
        script.command("docker network create --driver bridge --attachable " + networkName);
        script.runSuccessfully();
    }

    private void connectBridgeNetwork() {
        logger.info("Connecting sandbox bridge network");
        BashScript script = createDockerHostScript("connect_sandbox_network");
        script.command("docker network connect " + networkName + " " + runnerContainerId);
        script.runSuccessfully();
    }

    private void inspectBridgeNetwork() {
        logger.info("Inspecting sandbox bridge network");
        BashScript script = createDockerHostScript("inspect_sandbox_network");
        script.command("docker network inspect " + networkName);
        script.runSuccessfully();
    }

    private void createContainer() {
        logger.info("Creating sandbox container");

        List<String> containerOptionList = new ArrayList<>();
        containerOptionList.add("--runtime sysbox-runc");
        containerOptionList.add("--name " + containerName);
        containerOptionList.add("--tty");
        containerOptionList.add("--rm");
        containerOptionList.add("-v " + dockerHostWorkspaceDirectoryPath + ":" + workspaceDirectoryPath);
        if (fromHost) {
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
            logger.info("Starting sandbox container");
            BashScript script = createDockerHostScript("start_sandbox_container");
            script.command("docker container start --attach " + containerName);
            script.forEachOutput(logger::info);
            script.forEachError(logger::error);
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
            if (!socketReady)
                socketReady = socketIsReady();
            if (!daemonRunning)
                daemonRunning = daemonIsRunning();
            logger.debug("Waiting for sandbox container to be ready");
            Thread.sleep(1000);
        }
        logger.info("Sandbox container is ready");
    }

    private boolean socketIsReady() {
        try (Socket s = new Socket(socketIp, 2375)) {
            logger.info("Sandbox socket is ready");
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean daemonIsRunning() {
        BashScript script = new BashScript("check_docker_daemon");
        script.env("DOCKER_HOST=" + socketAddress);
        script.command("docker ps");
        script.run();
        if (script.getExitCode() == 0) {
            logger.info("Sandbox daemon is running");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getSocketAddress() {
        waitUntilSandboxIsReady();
        return socketAddress;
    }

    private void waitUntilSandboxIsReady() {
        try {
            getReady.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted when waiting for sandbox to be ready", e);
        }
    }

    public void shutdown() {
        logger.info("Shutting down sandbox");
        getReady.interrupt();
        BashScript script = createDockerHostScript("stop_sandbox_container");
        script.command("docker stop " + containerName);
        if (fromHost) {
            script.command("docker network disconnect " + networkName + " " + runnerContainerId);
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
