package plankton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;

@Component
public class PlanktonSetup {

    @Getter
    private final boolean runningFromHost;

    @Getter
    private final String runningFromContainerId;

    @Getter
    private final String dockerHostSocketAddress;

    @Getter
    private final boolean sandboxEnabled;

    @Getter
    private final String projectDirectoryPath;

    @Getter
    private final String projectDirectoryPathOnHost;

    @Getter
    private final String projectDirectoryPathOnSandbox;

    @Getter
    private final String projectDirectoryTargetPath;

    private final String composeFileRelativePath;
    private final String composeDirectoryRelativePath;

    @Getter
    private final String composeFileSourcePath;

    private final String composeDirectorySourcePath;
    private final String composeFileTargetPath;

    @Getter
    private final String composeDirectoryTargetPath;

    private static final Logger logger = LoggerFactory.getLogger(PlanktonSetup.class);

    public PlanktonSetup(@Autowired PlanktonConfiguration configuration) throws BashScriptFailedException {

        runningFromContainerId = getCurrentContainerId();
        logger.debug("runningFromContainerId={}", runningFromContainerId);

        runningFromHost = (runningFromContainerId == null);
        logger.debug("runningFromHost={}", runningFromHost);

        projectDirectoryPath = projectDirectoryPath(configuration.getProjectDirectory());
        logger.debug("projectDirectoryPath={}", projectDirectoryPath);

        composeFileSourcePath = composeFileSourcePath(configuration.getComposeFile());
        logger.debug("composeFileSourcePath={}", composeFileSourcePath);

        composeDirectorySourcePath = BashScript.runAndGetOutputString("dirname " + composeFileSourcePath);
        logger.debug("composeDirectorySourcePath={}", composeDirectorySourcePath);

        composeFileRelativePath = composeFileSourcePath.substring(projectDirectoryPath.length());
        logger.debug("composeFileRelativePath={}", composeFileRelativePath);

        composeDirectoryRelativePath = composeDirectorySourcePath.substring(projectDirectoryPath.length());
        logger.debug("composeDirectoryRelativePath={}", composeDirectoryRelativePath);

        if (runningFromHost)
            projectDirectoryPathOnHost = projectDirectoryPath;
        else
            projectDirectoryPathOnHost = configuration.getProjectDirectoryOnHost();
        logger.debug("projectDirectoryPathOnHost={}", projectDirectoryPathOnHost);
        // TODO it is possible to compute this, looking for the bind source
        // consider that it can be a super-directory

        dockerHostSocketAddress = configuration.getDockerHost();
        sandboxEnabled = configuration.isSandboxEnabled();
        if (sandboxEnabled) {
            projectDirectoryPathOnSandbox = "/sandbox-workspace";
            projectDirectoryTargetPath = projectDirectoryPathOnSandbox;
            composeFileTargetPath = projectDirectoryPathOnSandbox + "/" + composeFileRelativePath;
            composeDirectoryTargetPath = projectDirectoryPathOnSandbox + "/" + composeDirectoryRelativePath;
        } else {
            projectDirectoryPathOnSandbox = null;
            projectDirectoryTargetPath = projectDirectoryPathOnHost;
            composeFileTargetPath = projectDirectoryPathOnHost + "/" + composeFileRelativePath;
            composeDirectoryTargetPath = projectDirectoryPathOnHost + "/" + composeDirectoryRelativePath;
        }

        if (runningFromHost) {
            logger.debug("{} ... Project directory: {}", this, projectDirectoryPath);
            logger.debug("{} ... Compose file: {}", this, composeFileSourcePath);
        } else {
            logger.debug("{} ... Running from container: {}", this, runningFromContainerId);
            logger.debug("{} ... Project directory on host: {}", this, projectDirectoryPathOnHost);
            logger.debug("{} ... Project directory on container: {}", this, projectDirectoryPath);
            logger.debug("{} ... Compose file on container: {}", this, composeFileSourcePath);
        }

        logger.debug("{} ... Docker host: {}", this, dockerHostSocketAddress);

        if (sandboxEnabled) {
            logger.debug("{} ... Sandbox is enabled", this);
            logger.debug("{} ... Project directory on sandbox: {}", this, projectDirectoryPathOnSandbox);
            logger.debug("{} ... Compose file on sandbox: {}", this, composeFileTargetPath);
        }
    }

    private String getCurrentContainerId() throws BashScriptFailedException {
        String containerId = BashScript
                .runAndGetOutputString("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        if (containerId.isBlank())
            return null;
        else
            return containerId;
    }

    private String projectDirectoryPath(String projectDirectory) throws BashScriptFailedException {
        if (projectDirectory == null || projectDirectory.isBlank())
            projectDirectory = ".";
        return BashScript.runAndGetOutputString("realpath " + projectDirectory);
        // TODO check if directory exist
    }

    private String composeFileSourcePath(String composeFile) throws BashScriptFailedException {
        if (composeFile == null || composeFile.isBlank()) {
            composeFile = "plankton-compose.yaml";
            return BashScript.runAndGetOutputString("cd " + projectDirectoryPath + " && realpath " + composeFile);
        }
        return BashScript.runAndGetOutputString("realpath " + composeFile);

        // TODO alternative file names:
        // plankton-compose.yaml
        // plankton-compose.yml
        // compose.yaml
        // compose.yml

        // TODO check if file exist
        // TODO check if composeFileSourcePath start with projectDirectoryPath
    }

    @Override
    public String toString() {
        return PlanktonSetup.class.getSimpleName();
    }
}
