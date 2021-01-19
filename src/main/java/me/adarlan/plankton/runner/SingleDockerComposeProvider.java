package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.docker.DockerComposeConfiguration;
import me.adarlan.plankton.docker.DockerDaemon;

@Component
@ConditionalOnExpression("'${plankton.runner.mode}'=='single-pipeline' && ${plankton.docker:false}")
public class SingleDockerComposeProvider {

    @Autowired
    private PlanktonConfiguration planktonConfiguration;

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
            public String metadataDirectoryPath() {
                return planktonConfiguration.getDirectoryPath() + "/" + "docker";
            }
        });
    }
}
