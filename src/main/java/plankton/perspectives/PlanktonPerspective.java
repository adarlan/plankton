package plankton.perspectives;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.PlanktonConfiguration;
import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;

@Component
public class PlanktonPerspective {

    @Getter
    private final String projectPath;

    @Getter
    private final String composeFilePath;

    @Getter
    private final String composeDirectoryPath;

    @Getter
    private final String dockerHostSocketAddress;

    @Getter
    private final boolean runningFromHost;

    @Getter
    private final String runningFromContainerId;

    private static final Logger logger = LoggerFactory.getLogger(PlanktonPerspective.class);

    public PlanktonPerspective(@Autowired PlanktonConfiguration planktonConfiguration) {
        projectPath = initializeProjectPath(planktonConfiguration.getProjectDirectory());
        composeFilePath = initializeComposeFilePath(planktonConfiguration.getComposeFile());
        composeDirectoryPath = initializeComposeDirectoryPath();
        dockerHostSocketAddress = planktonConfiguration.getDockerHost();
        runningFromContainerId = getCurrentContainerId();
        runningFromHost = (runningFromContainerId == null);

        logger.debug("PlanktonPerspective.projectPath: {}", projectPath);
        logger.debug("PlanktonPerspective.composeFilePath: {}", composeFilePath);
        logger.debug("PlanktonPerspective.composeDirectoryPath: {}", composeDirectoryPath);
        logger.debug("PlanktonPerspective.dockerSocket: {}", dockerHostSocketAddress);
        logger.debug("PlanktonPerspective.runningFromContainerId: {}", runningFromContainerId);
        logger.debug("PlanktonPerspective.runningFromHost: {}", runningFromHost);
    }

    private String initializeProjectPath(String workspace) {
        if (isEmpty(workspace))
            workspace = ".";
        try {
            return BashScript.runAndGetOutputString("realpath " + workspace);
        } catch (BashScriptFailedException e) {
            throw new PerspectiveException("Unable to initialize the project path", e);
        }
        // TODO Check if directory exist
    }

    private String initializeComposeFilePath(String composeFile) {
        if (isEmpty(composeFile))
            composeFile = "plankton-compose.yaml";
        String result;
        try {
            result = BashScript.runAndGetOutputString("cd " + projectPath + " && realpath " + composeFile);
        } catch (BashScriptFailedException e) {
            throw new PerspectiveException("Unable to initialize the compose file path", e);
        }
        if (!result.startsWith(projectPath))
            throw new PerspectiveException("Compose file must be inside workspace");
        return result;

        // TODO Check if file exists

        // TODO Alternative file names:
        // - plankton-compose.yaml
        // - plankton-compose.yml
        // - compose.yaml
        // - compose.yml
    }

    private String initializeComposeDirectoryPath() {
        try {
            return BashScript.runAndGetOutputString("dirname " + composeFilePath);
        } catch (BashScriptFailedException e) {
            throw new PerspectiveException("Unable to initialize the compose directory path", e);
        }
    }

    private String getCurrentContainerId() {
        String containerId;
        try {
            containerId = BashScript
                    .runAndGetOutputString("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        } catch (BashScriptFailedException e) {
            throw new PerspectiveException("Unable to get current container id", e);
        }
        return isEmpty(containerId) ? null : containerId;
    }

    private boolean isEmpty(String string) {
        return string == null || string.isBlank();
    }
}
