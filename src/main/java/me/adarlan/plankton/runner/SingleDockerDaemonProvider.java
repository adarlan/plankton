package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.docker.DockerDaemon;
import me.adarlan.plankton.docker.DockerHost;
import me.adarlan.plankton.docker.DockerSandbox;
import me.adarlan.plankton.docker.DockerSandboxConfiguration;

@Component
public class SingleDockerDaemonProvider {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Bean
    public DockerDaemon dockerDaemon() {
        if (planktonSetup.isDockerSandboxEnabled()) {
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
                return planktonSetup.getPipelineId();
            }

            @Override
            public String dockerHostSocketAddress() {
                return planktonSetup.getDockerHostSocketAddress();
            }

            @Override
            public String underlyingWorkspaceDirectoryPath() {
                return planktonSetup.getUnderlyingWorkspaceDirectoryPath();
            }

            @Override
            public String workspaceDirectoryPath() {
                return planktonSetup.getWorkspaceDirectoryPath();
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
