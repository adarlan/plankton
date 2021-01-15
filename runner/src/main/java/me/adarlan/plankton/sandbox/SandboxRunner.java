package me.adarlan.plankton.sandbox;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.docker.DockerComposeConfiguration;
import me.adarlan.plankton.workflow.Pipeline;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;

public class SandboxRunner {

    private final String pipelineId;

    private final String runnerContainerName;

    private final String sandboxNetworkName;
    private final String sandboxContainerName;

    private final String workspaceDirectoryOnHost;
    private final String workspaceDirectoryOnSandbox;

    private final String workspaceDirectoryOnRunner;
    private final String composeFileOnRunner;
    private final String metadataDirectoryOnRunner;

    private final Thread sandboxContainerThread;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SandboxRunner(SandboxRunnerConfiguration configuration) {
        pipelineId = configuration.pipelineId();
        runnerContainerName = configuration.runnerContainerName();
        sandboxNetworkName = runnerContainerName + "_sandbox_network";
        sandboxContainerName = runnerContainerName + "_sandbox_container";
        workspaceDirectoryOnHost = configuration.workspaceDirectoryOnHost();
        workspaceDirectoryOnSandbox = "/workspace";
        workspaceDirectoryOnRunner = configuration.workspaceDirectoryOnRunner();
        composeFileOnRunner = configuration.composeFileOnRunner();
        metadataDirectoryOnRunner = configuration.metadataDirectoryOnRunner();
        sandboxContainerThread = sandboxContainerThread();
    }

    public void run() throws InterruptedException {
        createSandboxNetwork();
        connectSandboxNetwork();
        inspectSandboxNetwork();
        startSandboxContainer();
        checkSandboxContainer();
        inspectSandboxNetwork();
        runPipeline();
        stopSandboxContainer();
        // TODO disconnect sandbox network
        // TODO remove sandbox network
    }

    private void createSandboxNetwork() {
        BashScript script = new BashScript("createSandboxNetwork");
        script.command("docker network create --driver bridge --attachable " + sandboxNetworkName);
        script.runSuccessfully();
    }

    private void connectSandboxNetwork() {
        BashScript script = new BashScript("connectSandboxNetwork");
        script.command("docker network connect " + sandboxNetworkName + " " + runnerContainerName);
        script.runSuccessfully();
    }

    private void inspectSandboxNetwork() {
        BashScript script = new BashScript("inspectSandboxNetwork");
        script.command("docker network inspect " + sandboxNetworkName);
        script.runSuccessfully();
    }

    private void startSandboxContainer() {
        sandboxContainerThread.start();
    }

    private Thread sandboxContainerThread() {
        return new Thread(() -> {

            List<String> runOptionList = new ArrayList<>();
            runOptionList.add("--runtime sysbox-runc");
            runOptionList.add("--name " + sandboxContainerName);
            runOptionList.add("--tty");
            runOptionList.add("--rm");
            runOptionList.add("--network " + sandboxNetworkName);
            runOptionList.add("-v " + workspaceDirectoryOnHost + ":" + workspaceDirectoryOnSandbox);

            List<String> sandboxOptionList = new ArrayList<>();
            sandboxOptionList.add("-H tcp://0.0.0.0:2375");
            sandboxOptionList.add("-H unix:///var/run/docker.sock");

            String runOptions = runOptionList.stream().collect(Collectors.joining(" "));
            String sandboxOptions = sandboxOptionList.stream().collect(Collectors.joining(" "));

            BashScript script = new BashScript("run_sandbox_container");
            script.command("docker run " + runOptions + " adarlan/plankton:sandbox " + sandboxOptions);
            script.run();
            if (script.getExitCode() != 0) {
                throw new RuntimeException("Sandbox container failed");
            }
        });
    }

    private void checkSandboxContainer() throws InterruptedException {
        boolean sandboxReady = false;
        while (!sandboxReady) {
            try (Socket s = new Socket(sandboxContainerName, 2375)) {
                sandboxReady = true;
                logger.info("Sandbox container is ready");
            } catch (IOException ex) {
                logger.info("Waiting for sandbox container to be ready...");
                Thread.sleep(1000);
            }
        }
    }

    private DockerComposeConfiguration dockerComposeConfiguration() {
        return new DockerComposeConfiguration() {

            @Override
            public String projectName() {
                return pipelineId;
            }

            @Override
            public String filePath() {
                return composeFileOnRunner;
            }

            @Override
            public String projectDirectory() {
                return workspaceDirectoryOnRunner;
            }

            @Override
            public String metadataDirectory() {
                return metadataDirectoryOnRunner;
            }

            @Override
            public String dockerHost() {
                return "tcp://" + sandboxContainerName + ":2375";
            }
        };
    }

    private Compose compose() {
        return new DockerCompose(dockerComposeConfiguration());
    }

    private Pipeline pipeline() {
        return new Pipeline(compose());
    }

    private void runPipeline() throws InterruptedException {
        Pipeline pipeline = pipeline();
        pipeline.run();
    }

    private void stopSandboxContainer() {
        BashScript script = new BashScript("stop_sandbox_container");
        script.command("docker stop " + sandboxContainerName);
        script.runSuccessfully();
    }
}
