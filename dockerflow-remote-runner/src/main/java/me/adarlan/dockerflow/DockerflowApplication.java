package me.adarlan.dockerflow;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.adarlan.dockerflow.bash.BashScript;

public class DockerflowApplication {

    private static Logger logger;

    private static String runnerId;
    private static String runnerDirectoryOnHost;
    private static String runnerDirectoryOnRunner;

    private static String customerId = null;
    private static String dindVolumeName;

    private static String pipelineId;
    private static String pipelineDirectoryOnHost;
    private static String pipelineDirectoryOnRunner;
    private static String metadataDirectoryOnHost;
    private static String metadataDirectoryOnRunner;

    private static String dindNetworkName;
    private static String dindContainerName;

    private static String workspaceDirectoryOnHost;
    private static String workspaceDirectoryOnRunner;
    private static String workspaceDirectoryOnDind;
    private static String secretsDirectoryOnHost;
    private static String secretsDirectoryOnRunner;
    private static String secretsDirectoryOnDind;

    private static Thread runDindContainer;

    private static String gitUrl;
    private static String dockerflowFile;

    public static void main(String[] args) throws InterruptedException {

        Logger.setLevel(Logger.Level.TRACE);
        logger = Logger.getLogger();

        runnerId = System.getenv("RUNNER_ID");
        runnerDirectoryOnHost = System.getenv("RUNNER_EXTERNAL_DIRECTORY");
        runnerDirectoryOnRunner = System.getenv("RUNNER_INTERNAL_DIRECTORY");

        initializeCustomerId();
        if (customerId != null) {
            dindVolumeName = runnerId + "_" + customerId;
            createDindVolume();
        }

        initializePipelineId();

        pipelineDirectoryOnHost = runnerDirectoryOnHost + "/" + pipelineId;
        pipelineDirectoryOnRunner = runnerDirectoryOnRunner + "/" + pipelineId;
        metadataDirectoryOnHost = pipelineDirectoryOnHost + "/metadata";
        metadataDirectoryOnRunner = pipelineDirectoryOnRunner + "/metadata";

        dindNetworkName = runnerId + "_" + pipelineId;
        dindContainerName = runnerId + "_" + pipelineId;

        String slashWorkspace = "/workspace";
        workspaceDirectoryOnHost = pipelineDirectoryOnHost + slashWorkspace;
        workspaceDirectoryOnRunner = pipelineDirectoryOnRunner + slashWorkspace;
        workspaceDirectoryOnDind = slashWorkspace;

        String slashSecrets = "/secrets";
        secretsDirectoryOnHost = pipelineDirectoryOnHost + slashSecrets;
        secretsDirectoryOnRunner = pipelineDirectoryOnRunner + slashSecrets;
        secretsDirectoryOnDind = slashSecrets;

        cleanContainers();
        cleanNetworks();

        createDirectories();
        createDindNetwork();
        connectDindNetwork();
        inspectDindNetwork();
        runDindContainer();
        checkDindContainer();
        if (customerId != null) {
            cleanDindContainer();
        }
        inspectDindNetwork();

        gitUrl = "/test";
        dockerflowFile = "foo.docker-compose.yml";
        cloneWorkspace();
        makeSecrets();
        runPipeline();

        stopDindContainer();
        // disconnectDindNetwork();
        // removeDindNetwork();
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopDindContainer();
                cleanContainers();
                cleanNetworks();
            }
        });
    }

    // ==========

    private static void initializeCustomerId() {
        customerId = "public";
    }

    private static void createDindVolume() {
        // it will create only once
        BashScript script = new BashScript("createDindVolume");
        script.command("docker volume create " + dindVolumeName);
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
        logger.debug(() -> "pipelineId = " + pipelineId);
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

    private static void createDindNetwork() {
        BashScript script = new BashScript("createDindNetwork");
        script.command("docker network create --driver bridge --attachable " + dindNetworkName);
        script.runSuccessfully();
    }

    private static void connectDindNetwork() {
        BashScript script = new BashScript("connectDindNetwork");
        script.command("docker network connect " + dindNetworkName + " " + pipelineId);
        script.runSuccessfully();
    }

    private static void inspectDindNetwork() {
        BashScript script = new BashScript("inspectDindNetwork");
        script.command("docker network inspect " + dindNetworkName);
        script.runSuccessfully();
    }

    private static void runDindContainer() {
        runDindContainer = new RunDindContainer();
        runDindContainer.start();
    }

    private static class RunDindContainer extends Thread {
        private RunDindContainer() {
        }

        @Override
        public void run() {
            BashScript script = new BashScript("runDindContainer");
            List<String> runOptions = new ArrayList<>();
            runOptions.add("--runtime sysbox-runc");
            runOptions.add("--name " + dindContainerName);
            runOptions.add("--tty");
            runOptions.add("--rm");
            runOptions.add("--network " + dindNetworkName);
            runOptions.add("-v " + workspaceDirectoryOnHost + ":" + workspaceDirectoryOnDind);
            runOptions.add("-v " + secretsDirectoryOnHost + ":" + secretsDirectoryOnDind);
            if (customerId != null) {
                runOptions.add("--mount source=" + dindVolumeName + ",target=/var/lib/docker");
            }
            List<String> dockerdOptions = new ArrayList<>();
            dockerdOptions.add("-H tcp://0.0.0.0:2375");
            dockerdOptions.add("-H unix:///var/run/docker.sock");
            script.command("docker run " + runOptions.stream().collect(Collectors.joining(" ")) + " dockerflow:dind "
                    + dockerdOptions.stream().collect(Collectors.joining(" ")));
            script.run();
            if (script.getExitCode() != 0) {
                throw new DockerflowException("Unable to runDindContainer");
            }
        }
    }

    private static void checkDindContainer() throws InterruptedException {
        boolean dindReady = false;
        while (!dindReady) {
            try (Socket s = new Socket(dindContainerName, 2375)) {
                dindReady = true;
                logger.debug(() -> "Dind container is ready");
            } catch (IOException ex) {
                logger.debug(() -> "Waiting for dind container to be ready");
                Thread.sleep(1000);
            }
        }
    }

    private static void cleanDindContainer() {
        BashScript script = new BashScript("cleanDindContainer");
        script.env("DOCKER_HOST=tcp://" + dindContainerName + ":2375");
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

    private static void runPipeline() throws InterruptedException {
        DockerflowConfig config = new DockerflowConfig();
        config.setName(pipelineId);
        config.setFile(workspaceDirectoryOnRunner + "/" + dockerflowFile);
        config.setWorkspace(workspaceDirectoryOnRunner);
        config.setMetadata(metadataDirectoryOnRunner);
        config.setDockerHost("tcp://" + dindContainerName + ":2375");
        DockerCompose dockerCompose = new DockerCompose(config);
        Pipeline pipeline = new Pipeline(dockerCompose);
        pipeline.run();
    }

    // ==========

    private static void stopDindContainer() {
        BashScript script = new BashScript("stopDindContainer");
        script.command("docker stop " + dindContainerName);
        script.runSuccessfully();
    }
}
