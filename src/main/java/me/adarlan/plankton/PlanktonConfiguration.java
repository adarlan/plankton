package me.adarlan.plankton;

import java.time.Instant;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import me.adarlan.plankton.bash.BashScript;

public class PlanktonConfiguration {

    private static final String PLANKTON_DIRECTORY_PATH = "/var/lib/plankton";

    @Getter
    private final String id;

    @Getter
    private final String directoryPath;

    @Getter
    private final String composeFilePath;

    @Getter
    private final String workspaceDirectoryPath;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PlanktonConfiguration(String composeFilePath, Consumer<String> workspaceMaker) {
        this.id = String.valueOf(Instant.now().getEpochSecond());
        this.directoryPath = PLANKTON_DIRECTORY_PATH + "/" + id;
        this.composeFilePath = directoryPath + "/compose.yaml";
        this.workspaceDirectoryPath = directoryPath + "/workspace";
        createDirectory();
        copyComposeFile(composeFilePath, this.composeFilePath);
        createWorkspaceDirectory();
        workspaceMaker.accept(workspaceDirectoryPath);
    }

    private void createDirectory() {
        logger.info("Creating pipeline directory: {}", directoryPath);
        BashScript script = new BashScript("create_pipeline_directory");
        script.command("mkdir -p " + directoryPath);
        script.runSuccessfully();
    }

    private void copyComposeFile(String fromPath, String toPath) {
        logger.info("Copying Compose file from {} to {}", fromPath, toPath);
        BashScript script = new BashScript("copy_compose_file");
        script.command("cp " + fromPath + " " + toPath);
        script.runSuccessfully();
    }

    private void createWorkspaceDirectory() {
        logger.info("Creating workspace directory: {}", directoryPath);
        BashScript script = new BashScript("create_workspace_directory");
        script.command("mkdir -p " + directoryPath);
        script.runSuccessfully();
    }
}
