package me.adarlan.plankton.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.PlanktonConfiguration;
import me.adarlan.plankton.docker.DockerDaemon;
import me.adarlan.plankton.docker.DockerHost;
import me.adarlan.plankton.docker.DockerHostConfiguration;
import me.adarlan.plankton.docker.DockerSandbox;
import me.adarlan.plankton.docker.DockerSandboxConfiguration;

@Component
@ConditionalOnExpression("'${plankton.runner.mode}'=='single-pipeline' && ${plankton.docker:false}")
public class SingleDockerDaemonProvider {

    @Value("${plankton.docker.sandbox}")
    private boolean sandboxEnabled;

    @Value("${plankton.docker.host}")
    private String dockerHostSocketAddress;

    @Autowired
    private PlanktonConfiguration planktonConfiguration;

    @Bean
    public DockerDaemon dockerDaemon() {
        if (sandboxEnabled) {
            return dockerSandbox();
        } else {
            return dockerHost();
        }
    }

    private DockerHost dockerHost() {
        return new DockerHost(dockerHostConfiguration());
    }

    private DockerSandbox dockerSandbox() {
        return new DockerSandbox(new DockerSandboxConfiguration() {

            @Override
            public String id() {
                return planktonConfiguration.getId();
            }

            @Override
            public DockerHostConfiguration dockerHostConfiguration() {
                return SingleDockerDaemonProvider.this.dockerHostConfiguration();
            }

            @Override
            public String workspaceDirectoryPath() {
                return planktonConfiguration.getWorkspaceDirectoryPath();
            }
        });
    }

    private DockerHostConfiguration dockerHostConfiguration() {
        return new DockerHostConfiguration() {

            @Override
            public String socketAddress() {
                return dockerHostSocketAddress;
            }

            @Override
            public String workspaceDirectoryPath() {
                return planktonConfiguration.getWorkspaceDirectoryPath();
            }
        };
    }
}
