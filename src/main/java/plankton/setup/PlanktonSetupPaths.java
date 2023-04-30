package plankton.setup;

import java.io.IOException;
import java.nio.file.Paths;

import lombok.Getter;
import plankton.docker.client.DockerClient;
import plankton.docker.inspect.Container;
import plankton.docker.inspect.ContainerMount;
import plankton.docker.inspect.DockerInspect;

public class PlanktonSetupPaths {

    @Getter
    private final String workspacePathFromPlanktonPerspective;

    @Getter
    private final String workspacePathOnHost;

    @Getter
    private final String workspacePathOnSandbox;

    @Getter
    private final String workspacePathFromAdapterPerspective;

    @Getter
    private final String composeFilePathFromPlanktonPerspective;

    @Getter
    private final String composeFilePathRelativeToWorkspace;

    @Getter
    private final String composeFilePathFromAdapterPerspective;

    public PlanktonSetupPaths(
            PlanktonSetup setup,
            PlanktonSetupRunningFrom runningFrom,
            PlanktonSetupSandboxMode sandboxMode,
            PlanktonSetupDockerHost dockerHost) {

        String workspace = workspace(setup.getWorkspace());
        String file = file(setup.getFile());

        workspacePathFromPlanktonPerspective = workspacePathFromPlanktonPerspective(workspace);
        composeFilePathFromPlanktonPerspective = composeFilePathFromPlanktonPerspective(file);
        composeFilePathRelativeToWorkspace = composeFilePathRelativeToWorkspace();

        if (runningFrom.isRunningFromHost()) {
            workspacePathOnHost = workspacePathFromPlanktonPerspective;
        } else {
            workspacePathOnHost = workspacePathOnHostWhenRunningFromContainer(
                    dockerHost.getDockerHostClient(),
                    runningFrom.getRunningFromContainerId());
        }
        if (sandboxMode.isSandboxEnabled()) {
            workspacePathOnSandbox = "/sandbox-workspace";
            workspacePathFromAdapterPerspective = workspacePathOnSandbox;
        } else {
            workspacePathOnSandbox = null;
            workspacePathFromAdapterPerspective = workspacePathOnHost;
        }
        composeFilePathFromAdapterPerspective = workspacePathFromAdapterPerspective
                + composeFilePathRelativeToWorkspace;
    }

    private String workspace(String ws) {
        return (ws == null || ws.isBlank())
                ? "."
                : ws;
    }

    private String file(String f) {
        return (f == null || f.isBlank())
                ? "plankton.yaml"
                : f;
    }

    private String workspacePathFromPlanktonPerspective(String workspace) {
        try {
            return Paths.get(workspace).toAbsolutePath().toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new PlanktonSetupException("Unable to initialize the workspace path from Plankton perspective", e);
        }
        // TODO Check if directory exist
    }

    private String composeFilePathFromPlanktonPerspective(String file) {
        String result;
        try {
            result = Paths.get(workspacePathFromPlanktonPerspective)
                    .resolve(Paths.get(file)).toAbsolutePath().toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new PlanktonSetupException("Unable to initialize the compose file path from Plankton perspective", e);
        }
        if (!result.startsWith(workspacePathFromPlanktonPerspective))
            throw new PlanktonSetupException("Compose file must be inside workspace");
        return result;
        // TODO Check if file exists
    }

    private String composeFilePathRelativeToWorkspace() {
        return composeFilePathFromPlanktonPerspective.substring(workspacePathFromPlanktonPerspective.length());
    }

    private String workspacePathOnHostWhenRunningFromContainer(DockerClient dockerHostClient,
            String runningFromContainerId) {
        DockerInspect dockerInspect = new DockerInspect(dockerHostClient);
        Container container = dockerInspect.getContainer(runningFromContainerId);
        for (ContainerMount mount : container.getMounts()) {
            if (mount.getDestination().equals(workspacePathFromPlanktonPerspective)) {
                return mount.getSource();
            }
        }
        throw new PlanktonSetupException(
                "When running Plankton from within a container, you must bind the workspace directory");
        // TODO what if destination is a super directory of project path?
    }
}
