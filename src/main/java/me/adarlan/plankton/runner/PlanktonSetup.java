package me.adarlan.plankton.runner;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;

@Configuration
public class PlanktonSetup {

    @Getter
    private final boolean dockerEnabled;

    @Getter
    private final String dockerHostSocketAddress;

    @Getter
    private final boolean dockerSandboxEnabled;

    @Getter
    private final String metadataDirectoryPath;

    @Getter
    private final String pipelineId;

    @Getter
    private final String pipelineDirectoryPath;

    @Getter
    private final String composeFilePath;

    @Getter
    private final String workspaceDirectoryPath;

    @Getter
    private final String underlyingWorkspaceDirectoryPath;

    @Getter
    private final String containerStateDirectoryPath;

    @Getter
    private final boolean runningFromHost;

    @Getter
    private final String runningFromContainerId;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlanktonSetup(@Autowired PlanktonConfiguration planktonConfiguration) {

        logger.info("Setting up Plankton");

        this.dockerEnabled = true;
        this.dockerHostSocketAddress = planktonConfiguration.getDockerHost();
        this.dockerSandboxEnabled = planktonConfiguration.isDockerSandboxEnabled();
        this.metadataDirectoryPath = planktonConfiguration.getMetadataDirectory();
        this.pipelineId = String.valueOf(Instant.now().getEpochSecond());
        this.pipelineDirectoryPath = metadataDirectoryPath + "/" + pipelineId;
        this.composeFilePath = pipelineDirectoryPath + "/" + "plankton.compose.yaml";
        this.workspaceDirectoryPath = pipelineDirectoryPath + "/" + "workspace";
        this.underlyingWorkspaceDirectoryPath = planktonConfiguration.getMetadataDirectoryUnderlying() + "/"
                + pipelineId + "/" + "workspace";
        this.containerStateDirectoryPath = pipelineDirectoryPath + "/" + "container-state";

        if (dockerEnabled) {
            logger.info("dockerHostSocketAddress={}", dockerHostSocketAddress);
            logger.info("dockerSandboxEnabled={}", dockerSandboxEnabled);
        }
        logger.info("metadataDirectoryPath={}", metadataDirectoryPath);
        logger.info("pipelineId={}", pipelineId);
        logger.info("pipelineDirectoryPath={}", pipelineDirectoryPath);
        logger.info("composeFilePath={}", composeFilePath);
        logger.info("workspaceDirectoryPath={}", workspaceDirectoryPath);
        logger.info("underlyingWorkspaceDirectoryPath={}", underlyingWorkspaceDirectoryPath);
        logger.info("containerStateDirectoryPath={}", containerStateDirectoryPath);

        this.runningFromContainerId = runningFromContainerId();
        this.runningFromHost = runningFromContainerId.isBlank();

        if (runningFromHost) {
            logger.info("Running Plankton directly from host");
        } else {
            logger.info("Running Plankton from within a container: {}", runningFromContainerId);
        }

        createDirectory(metadataDirectoryPath);
        createDirectory(pipelineDirectoryPath);
        createDirectory(workspaceDirectoryPath);
        createDirectory(containerStateDirectoryPath);
        copyComposeFileFrom(planktonConfiguration.getComposeFile());
        copyWorkspaceFrom(planktonConfiguration.getProjectDirectory());
    }

    private String runningFromContainerId() {
        try {
            return BashScript.runAndGetOutputString("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        } catch (BashScriptFailedException e) {
            return "";
        }
    }

    private void createDirectory(String path) {
        logger.info("Creating directory: {}", path);
        try {
            BashScript.run("mkdir -p " + path);
        } catch (BashScriptFailedException e) {
            throw new PlanktonRunnerException("Unable to create directory", e);
        }
    }

    private void copyComposeFileFrom(String fromPath) {
        logger.info("Copying Compose file from {} to {}", fromPath, composeFilePath);
        try {
            BashScript.run("cp " + fromPath + " " + composeFilePath);
        } catch (BashScriptFailedException e) {
            throw new PlanktonRunnerException("Unable to copy Compose file", e);
        }
    }

    private void copyWorkspaceFrom(String fromPath) {
        logger.info("Copying workspace files from {} to {}", fromPath, workspaceDirectoryPath);
        try {
            BashScript.run("cp -R " + fromPath + "/. " + workspaceDirectoryPath + "/");
        } catch (BashScriptFailedException e) {
            throw new PlanktonRunnerException("Unable to copy workspace files", e);
        }
    }
}
