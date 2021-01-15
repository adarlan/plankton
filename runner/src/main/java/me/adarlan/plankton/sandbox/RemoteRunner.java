package me.adarlan.plankton.sandbox;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.docker.DockerComposeConfiguration;
import me.adarlan.plankton.workflow.Pipeline;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.compose.Compose;

public class RemoteRunner {

    private static String runnerId;
    private static String runnerDirectoryOnHost;
    private static String runnerDirectoryOnRunner;

    private static String customerId = null;
    private static String sandboxVolumeName;

    private static String pipelineId;
    private static String pipelineDirectoryOnHost;
    private static String pipelineDirectoryOnRunner;
    private static String metadataDirectoryOnHost;
    private static String metadataDirectoryOnRunner;

    private static String sandboxNetworkName;
    private static String sandboxContainerName;

    private static String workspaceDirectoryOnHost;
    private static String workspaceDirectoryOnRunner;
    private static String workspaceDirectoryOnSandbox;
    private static String secretsDirectoryOnHost;
    private static String secretsDirectoryOnRunner;
    private static String secretsDirectoryOnSandbox;

    private static Thread runSandboxContainer;

    private static String gitUrl;
    private static String planktonFile;

    public static void main(String[] args) throws InterruptedException {

        runnerId = System.getenv("RUNNER_ID");
        runnerDirectoryOnHost = System.getenv("RUNNER_EXTERNAL_DIRECTORY");
        runnerDirectoryOnRunner = System.getenv("RUNNER_INTERNAL_DIRECTORY");

        initializeCustomerId();
        if (customerId != null) {
            sandboxVolumeName = runnerId + "_" + customerId;
            createSandboxVolume();
        }

        initializePipelineId();

        pipelineDirectoryOnHost = runnerDirectoryOnHost + "/" + pipelineId;
        pipelineDirectoryOnRunner = runnerDirectoryOnRunner + "/" + pipelineId;
        metadataDirectoryOnHost = pipelineDirectoryOnHost + "/metadata";
        metadataDirectoryOnRunner = pipelineDirectoryOnRunner + "/metadata";

        sandboxNetworkName = runnerId + "_" + pipelineId;
        sandboxContainerName = runnerId + "_" + pipelineId;

        String slashWorkspace = "/workspace";
        workspaceDirectoryOnHost = pipelineDirectoryOnHost + slashWorkspace;
        workspaceDirectoryOnRunner = pipelineDirectoryOnRunner + slashWorkspace;
        workspaceDirectoryOnSandbox = slashWorkspace;

        String slashSecrets = "/secrets";
        secretsDirectoryOnHost = pipelineDirectoryOnHost + slashSecrets;
        secretsDirectoryOnRunner = pipelineDirectoryOnRunner + slashSecrets;
        secretsDirectoryOnSandbox = slashSecrets;

        cleanContainers();
        cleanNetworks();

        createDirectories();
        createSandboxNetwork();
        connectSandboxNetwork();
        inspectSandboxNetwork();
        runSandboxContainer();
        checkSandboxContainer();
        if (customerId != null) {
            cleanSandboxContainer();
        }
        inspectSandboxNetwork();

        gitUrl = "/test";
        planktonFile = "foo.docker-compose.yml";
        cloneWorkspace();
        makeSecrets();
        runPipeline();

        stopSandboxContainer();
        // disconnectSandboxNetwork();
        // removeSandboxNetwork();
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopSandboxContainer();
                cleanContainers();
                cleanNetworks();
            }
        });
    }

    // ==========

    private static void initializeCustomerId() {
        customerId = "public";
    }

    private static void createSandboxVolume() {
        // it will create only once
        BashScript script = new BashScript("createSandboxVolume");
        script.command("docker volume create " + sandboxVolumeName);
        script.runSuccessfully();
        // TODO Esse volume tem que ser compartilhado entre os workers,
        // pois não se sabe em qual worker o runner vai rodar cada pipeline.
        // E se o volume não existir, copiar de outro runner (caso exista).
    }

    // ==========

    private static void initializePipelineId() {
        List<String> scriptOutput = new ArrayList<>();
        BashScript script = new BashScript("initializePipelineId");
        script.command("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        script.forEachOutput(scriptOutput::add);
        script.runSuccessfully();
        pipelineId = scriptOutput.stream().collect(Collectors.joining()).substring(0, 12);
        System.out.println("pipelineId = " + pipelineId);
    }

    private static void cleanContainers() {
        if (runnerId == null) {
            return;
        }
        BashScript script = new BashScript("cleanContainers");
        script.command("if Containers=\"$(docker ps --format {{.Names}} | grep " + runnerId + "_" + ")\"; then");
        script.command("  docker stop $Containers");
        script.command("  docker rm $Containers");
        script.command("fi");
        script.runSuccessfully();
    }

    private static void cleanNetworks() {
        if (runnerId == null) {
            return;
        }
        BashScript script = new BashScript("cleanNetworks");
        script.command("if Networks=\"$(docker network ls --format {{.Name}} | grep " + runnerId + "_" + ")\"; then");
        script.command("  docker network rm $Networks");
        script.command("fi");
        script.runSuccessfully();
    }

    // ==========

    private static void createDirectories() {
        BashScript script = new BashScript("createDirectories");
        String mkdir = "mkdir -p ";
        script.command(mkdir + pipelineDirectoryOnRunner);
        script.command(mkdir + metadataDirectoryOnRunner);
        script.command(mkdir + workspaceDirectoryOnRunner);
        script.command(mkdir + secretsDirectoryOnRunner);
        script.runSuccessfully();
    }

    private static void createSandboxNetwork() {
        BashScript script = new BashScript("createSandboxNetwork");
        script.command("docker network create --driver bridge --attachable " + sandboxNetworkName);
        script.runSuccessfully();
    }

    private static void connectSandboxNetwork() {
        BashScript script = new BashScript("connectSandboxNetwork");
        script.command("docker network connect " + sandboxNetworkName + " " + pipelineId);
        script.runSuccessfully();
    }

    private static void inspectSandboxNetwork() {
        BashScript script = new BashScript("inspectSandboxNetwork");
        script.command("docker network inspect " + sandboxNetworkName);
        script.runSuccessfully();
    }

    private static void runSandboxContainer() {
        runSandboxContainer = new RunSandboxContainer();
        runSandboxContainer.start();
    }

    private static class RunSandboxContainer extends Thread {
        private RunSandboxContainer() {
        }

        @Override
        public void run() {
            BashScript script = new BashScript("runSandboxContainer");
            List<String> runOptions = new ArrayList<>();
            runOptions.add("--runtime sysbox-runc");
            runOptions.add("--name " + sandboxContainerName);
            runOptions.add("--tty");
            runOptions.add("--rm");
            runOptions.add("--network " + sandboxNetworkName);
            runOptions.add("-v " + workspaceDirectoryOnHost + ":" + workspaceDirectoryOnSandbox);
            runOptions.add("-v " + secretsDirectoryOnHost + ":" + secretsDirectoryOnSandbox);
            if (customerId != null) {
                runOptions.add("--mount source=" + sandboxVolumeName + ",target=/var/lib/docker");
            }
            List<String> dockerdOptions = new ArrayList<>();
            dockerdOptions.add("-H tcp://0.0.0.0:2375");
            dockerdOptions.add("-H unix:///var/run/docker.sock");
            script.command("docker run " + runOptions.stream().collect(Collectors.joining(" "))
                    + " plankton:remote-runner-sandbox " + dockerdOptions.stream().collect(Collectors.joining(" ")));
            script.run();
            if (script.getExitCode() != 0) {
                throw new RuntimeException("Unable to runSandboxContainer");
                // TODO replace RuntimeException
            }
        }
    }

    private static void checkSandboxContainer() throws InterruptedException {
        boolean sandboxReady = false;
        while (!sandboxReady) {
            try (Socket s = new Socket(sandboxContainerName, 2375)) {
                sandboxReady = true;
                System.out.println("Sandbox container is ready");
            } catch (IOException ex) {
                System.out.println("Waiting for sandbox container to be ready");
                Thread.sleep(1000);
            }
        }
    }

    private static void cleanSandboxContainer() {
        BashScript script = new BashScript("cleanSandboxContainer");
        script.env("DOCKER_HOST=tcp://" + sandboxContainerName + ":2375");
        script.command("while Containers=\"$(docker ps -a --format {{.Names}} | grep " + runnerId + "_" + ")\"; do");
        script.command("  docker stop $Containers");
        script.command("  docker container prune -f");
        script.command("  sleep 1");
        script.command("done");
        script.command("docker network prune -f");
        script.command("docker volume prune -f");
        script.command("docker image prune -f"); // it removes only dangling images
        script.runSuccessfully();
    }

    // ==========

    private static void cloneWorkspace() {
        BashScript script = new BashScript("cloneWorkspace");
        script.command("cd " + workspaceDirectoryOnRunner);
        script.command("git clone " + gitUrl + " .");
        script.runSuccessfully();
    }

    private static void makeSecrets() {
        //
    }

    private static DockerComposeConfiguration dockerComposeConfiguration() {
        return new DockerComposeConfiguration() {

            @Override
            public String projectName() {
                return pipelineId;
            }

            @Override
            public String filePath() {
                return workspaceDirectoryOnRunner + "/" + planktonFile;
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

    private static Compose compose() {
        return new DockerCompose(dockerComposeConfiguration());
    }

    private static Pipeline pipeline() {
        return new Pipeline(compose());
    }

    private static void runPipeline() throws InterruptedException {
        Pipeline pipeline = pipeline();
        pipeline.run();
    }

    // ==========

    private static void stopSandboxContainer() {
        BashScript script = new BashScript("stopSandboxContainer");
        script.command("docker stop " + sandboxContainerName);
        script.runSuccessfully();
    }
}
