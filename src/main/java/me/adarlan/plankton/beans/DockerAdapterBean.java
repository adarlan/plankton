package me.adarlan.plankton.beans;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.core.ContainerRuntimeAdapter;
import me.adarlan.plankton.docker.DockerAdapter;
import me.adarlan.plankton.docker.DockerAdapterConfiguration;
import me.adarlan.plankton.docker.DockerDaemon;
import me.adarlan.plankton.PlanktonSetup;

@Component
public class DockerAdapterBean {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Autowired
    private DockerDaemon dockerDaemon;

    @Autowired
    private ComposeDocument composeDocument;

    @Bean
    public ContainerRuntimeAdapter containerRuntimeAdapter() {
        return new DockerAdapter(new DockerAdapterConfiguration() {

            @Override
            public DockerDaemon dockerDaemon() {
                return dockerDaemon;
            }

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public String namespace() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String projectDirectoryPath() {
                return planktonSetup.getProjectDirectoryPath();
            }

            @Override
            public String projectDirectoryTargetPath() {
                return planktonSetup.getProjectDirectoryTargetPath();
            }
        });
    }
}
