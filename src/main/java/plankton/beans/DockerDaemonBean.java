package plankton.beans;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.docker.daemon.DockerDaemon;
import plankton.docker.daemon.DockerHost;
import plankton.docker.daemon.DockerSandbox;
import plankton.docker.daemon.DockerSandboxConfiguration;
import plankton.perspectives.HostPerspective;
import plankton.perspectives.PlanktonPerspective;
import plankton.perspectives.SandboxPerspective;

@Component
public class DockerDaemonBean {

    @Autowired
    private HostPerspective hostPerspective;

    @Autowired
    private PlanktonPerspective planktonPerspective;

    @Autowired
    private SandboxPerspective sandboxPerspective;

    @Bean
    public DockerDaemon dockerDaemon() {
        if (sandboxPerspective.isSandboxEnabled()) {
            return dockerSandbox();
        } else {
            return dockerHost();
        }
    }

    private DockerHost dockerHost() {
        return new DockerHost(() -> planktonPerspective.getDockerHostSocketAddress());
    }

    private DockerSandbox dockerSandbox() {
        return new DockerSandbox(new DockerSandboxConfiguration() {

            @Override
            public String id() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String dockerHostSocketAddress() {
                return planktonPerspective.getDockerHostSocketAddress();
            }

            @Override
            public String underlyingWorkspaceDirectoryPath() {
                return hostPerspective.getProjectDirectoryPath();
            }

            @Override
            public String workspaceDirectoryPath() {
                return sandboxPerspective.getProjectDirectoryPathOnSandbox();
            }

            @Override
            public boolean runningFromHost() {
                return planktonPerspective.isRunningFromHost();
            }

            @Override
            public String runningFromContainerId() {
                return planktonPerspective.getRunningFromContainerId();
            }
        });
    }
}
