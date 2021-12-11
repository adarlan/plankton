package plankton.perspectives;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.PlanktonConfiguration;

@Component
public class SandboxPerspective {
    // TODO => BoxPerspective or ContainerRuntimePerspective

    @Getter
    private final boolean sandboxEnabled;

    @Getter
    private final String projectDirectoryPathOnSandbox;

    @Getter
    private final String projectDirectoryTargetPath;

    @Getter
    private final String composeFileTargetPath;

    @Getter
    private final String composeDirectoryTargetPath;

    private static final Logger logger = LoggerFactory.getLogger(SandboxPerspective.class);

    public SandboxPerspective(
            @Autowired PlanktonConfiguration planktonConfiguration,
            @Autowired WorkspacePerspective workspacePerspective,
            @Autowired HostPerspective hostPerspective) {
        sandboxEnabled = planktonConfiguration.isSandboxEnabled();
        if (sandboxEnabled) {
            projectDirectoryPathOnSandbox = "/sandbox-workspace";
            projectDirectoryTargetPath = projectDirectoryPathOnSandbox;
            composeFileTargetPath = projectDirectoryPathOnSandbox + "/"
                    + workspacePerspective.getComposeFileRelativePath();
            composeDirectoryTargetPath = projectDirectoryPathOnSandbox + "/"
                    + workspacePerspective.getComposeDirectoryRelativePath();
        } else {
            projectDirectoryPathOnSandbox = null;
            projectDirectoryTargetPath = hostPerspective.getProjectDirectoryPath();
            composeFileTargetPath = hostPerspective.getProjectDirectoryPath() + "/"
                    + workspacePerspective.getComposeFileRelativePath();
            composeDirectoryTargetPath = hostPerspective.getProjectDirectoryPath() + "/"
                    + workspacePerspective.getComposeDirectoryRelativePath();
        }

        logger.debug("SandboxPerspective.sandboxEnabled: {}", sandboxEnabled);
        logger.debug("SandboxPerspective.projectDirectoryPathOnSandbox: {}", projectDirectoryPathOnSandbox);
        logger.debug("SandboxPerspective.projectDirectoryTargetPath: {}", projectDirectoryTargetPath);
        logger.debug("SandboxPerspective.composeFileTargetPath: {}", composeFileTargetPath);
        logger.debug("SandboxPerspective.composeDirectoryTargetPath: {}", composeDirectoryTargetPath);
    }
}
