package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.docker.DockerComposeConfiguration;
import me.adarlan.plankton.docker.DockerDaemon;

@Component
public class SingleDockerComposeProvider {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Autowired
    private DockerDaemon dockerDaemon;

    @Autowired
    private ComposeDocument composeDocument;

    @Bean
    public Compose compose() {
        return new DockerCompose(new DockerComposeConfiguration() {

            @Override
            public DockerDaemon dockerDaemon() {
                return dockerDaemon;
            }

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public String containerStateDirectoryPath() {
                return planktonSetup.getContainerStateDirectoryPath();
            }
        });
    }
}
