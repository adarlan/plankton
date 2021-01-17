package me.adarlan.plankton.sandbox;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.adarlan.plankton.bash.BashScript;

public class Sandbox {

    private final String id;
    private boolean fromHost;

    private final String containerName;
    private final String networkName;

    private final String workspaceDirectorySource;
    private final String workspaceDirectoryTarget;

    private final String socketAddress;
    private final String socketIp;

    private final Thread getReady;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Sandbox(SandboxConfiguration configuration) {
        this.id = configuration.id();
        initializeMode();
        containerName = id + "_sandbox";
        if (fromHost) {
            networkName = null;
            socketIp = "127.0.0.1";
        } else {
            networkName = id + "_sandbox";
            socketIp = containerName;
        }
        socketAddress = "tcp://" + socketIp + ":2375";
        workspaceDirectorySource = configuration.workspaceDirectoryOnHost();
        workspaceDirectoryTarget = "/workspace";
        getReady = new Thread(() -> {
            if (!fromHost) {
                createNetwork();
                connectNetwork(id);
                inspectNetwork();
            }
            createContainer();
            startContainer();
            waitUntilSocketIsReady();
        });
        getReady.start();
    }

    private void initializeMode() {
        List<String> scriptOutput = new ArrayList<>();
        BashScript script = new BashScript("get_sandbox_container_id");
        script.command("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        script.forEachOutput(scriptOutput::add);
        script.runSuccessfully();
        fromHost = scriptOutput.isEmpty();
    }

    private void createNetwork() {
        BashScript script = new BashScript("create_sandbox_network");
        script.command("docker network create --driver bridge --attachable " + networkName);
        script.runSuccessfully();
    }

    private void connectNetwork(String runnerContainerName) {
        BashScript script = new BashScript("connect_sandbox_network");
        script.command("docker network connect " + networkName + " " + runnerContainerName);
        script.runSuccessfully();
    }

    private void inspectNetwork() {
        BashScript script = new BashScript("inspect_sandbox_network");
        script.command("docker network inspect " + networkName);
        script.runSuccessfully();
    }

    private void createContainer() {

        List<String> containerOptionList = new ArrayList<>();
        containerOptionList.add("--runtime sysbox-runc");
        containerOptionList.add("--name " + containerName);
        containerOptionList.add("--tty");
        containerOptionList.add("--rm");
        containerOptionList.add("-v " + workspaceDirectorySource + ":" + workspaceDirectoryTarget);
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

        BashScript script = new BashScript("create_sandbox_container");
        script.command("docker container create " + containerOptions + " adarlan/plankton:sandbox " + sandboxOptions);
        script.run();
        if (script.getExitCode() != 0) {
            throw new SandboxException(
                    "Unable to create sandbox container; Script exited with code: " + script.getExitCode());
        }
    }

    private void startContainer() {
        new Thread(() -> {
            BashScript script = new BashScript("start_sandbox_container");
            script.command("docker container start --attach " + containerName);
            script.forEachOutput(logger::info);
            script.forEachError(logger::error);
            script.run();
            if (script.getExitCode() != 0) {
                throw new SandboxException("Sandbox container exited with a non-zero code: " + script.getExitCode());
            }
        }).start();
    }

    private void waitUntilSocketIsReady() {
        while (!socketIsReady() || !daemonIsRunning()) {
            logger.debug("Waiting for sandbox container to be ready");
            try {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted when waiting for socket to be ready", e);
            }
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

    public void stop() {
        BashScript script = new BashScript("stop_sandbox_container");
        script.command("docker stop " + containerName);
        script.runSuccessfully();
        // TODO interrupt getReady thread
        // TODO disconnect sandbox network
        // TODO remove sandbox network
    }
}
