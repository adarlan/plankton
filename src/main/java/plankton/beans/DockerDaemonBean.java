package plankton.beans;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.docker.DockerDaemon;
import plankton.docker.DockerHost;
import plankton.docker.DockerSandbox;
import plankton.docker.DockerSandboxConfiguration;
import plankton.PlanktonSetup;

@Component
public class DockerDaemonBean {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Bean
    public DockerDaemon dockerDaemon() {
        if (planktonSetup.isSandboxEnabled()) {
            return dockerSandbox();
        } else {
            return dockerHost();
        }
    }

    private DockerHost dockerHost() {
        return new DockerHost(() -> planktonSetup.getDockerHostSocketAddress());
    }

    private DockerSandbox dockerSandbox() {
        return new DockerSandbox(new DockerSandboxConfiguration() {

            @Override
            public String id() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String dockerHostSocketAddress() {
                return planktonSetup.getDockerHostSocketAddress();
            }

            @Override
            public String underlyingWorkspaceDirectoryPath() {
                return planktonSetup.getProjectDirectoryPathOnHost();
            }

            @Override
            public String workspaceDirectoryPath() {
                return planktonSetup.getProjectDirectoryPathOnSandbox();
            }

            @Override
            public boolean runningFromHost() {
                return planktonSetup.isRunningFromHost();
            }

            @Override
            public String runningFromContainerId() {
                return planktonSetup.getRunningFromContainerId();
            }
        });
    }
}
