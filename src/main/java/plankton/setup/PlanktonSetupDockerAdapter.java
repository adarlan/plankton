package plankton.setup;

import lombok.Getter;
import plankton.docker.adapter.DockerAdapter;
import plankton.docker.adapter.DockerAdapterConfiguration;
import plankton.docker.daemon.DockerDaemon;
import plankton.docker.daemon.DockerSandboxConfiguration;
import plankton.docker.daemon.DockerSandboxDaemon;

public class PlanktonSetupDockerAdapter {

    private final DockerDaemon dockerAdapterDaemon;

    @Getter
    private final DockerAdapter dockerAdapter;

    public PlanktonSetupDockerAdapter(
            PlanktonSetupNamespace namespace,
            PlanktonSetupSandboxMode sandboxMode,
            PlanktonSetupDockerHost dockerHost,
            PlanktonSetupPaths paths,
            PlanktonSetupRunningFrom runningFrom) {

        if (sandboxMode.isSandboxEnabled()) {
            dockerAdapterDaemon = new DockerSandboxDaemon(new DockerSandboxConfiguration() {

                @Override
                public String namespace() {
                    return namespace.getNamespace();
                }

                @Override
                public String dockerHostSocketAddress() {
                    return dockerHost.getDockerHostSocketAddress();
                }

                @Override
                public String underlyingWorkspaceDirectoryPath() {
                    return paths.getWorkspacePathOnHost();
                }

                @Override
                public String workspaceDirectoryPath() {
                    return paths.getWorkspacePathOnHost();
                }

                @Override
                public boolean runningFromHost() {
                    return runningFrom.isRunningFromHost();
                }

                @Override
                public String runningFromContainerId() {
                    return runningFrom.getRunningFromContainerId();
                }
            });
        } else {
            dockerAdapterDaemon = dockerHost.getDockerHostDaemon();
        }

        dockerAdapter = new DockerAdapter(new DockerAdapterConfiguration() {

            @Override
            public DockerDaemon dockerDaemon() {
                return dockerAdapterDaemon;
            }

            @Override
            public String namespace() {
                return namespace.getNamespace();
            }

            @Override
            public String workspacePathFromRunnerPerspective() {
                return paths.getWorkspacePathFromPlanktonPerspective();
            }

            @Override
            public String workspacePathFromAdapterPerspective() {
                return paths.getWorkspacePathFromAdapterPerspective();
            }
        });
    }
}
