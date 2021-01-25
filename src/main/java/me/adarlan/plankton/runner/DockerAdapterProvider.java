package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.docker.DockerAdapter;
import me.adarlan.plankton.docker.DockerAdapterConfiguration;
import me.adarlan.plankton.docker.DockerDaemon;
import me.adarlan.plankton.pipeline.ContainerRuntimeAdapter;

@Component
public class DockerAdapterProvider {

    @Autowired
    private DockerDaemon dockerDaemon;

    @Autowired
    private ComposeDocument composeDocument;

    @Bean
    public ContainerRuntimeAdapter composeAdapter() {
        return new DockerAdapter(new DockerAdapterConfiguration() {

            @Override
            public DockerDaemon dockerDaemon() {
                return dockerDaemon;
            }

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }
        });
    }
}
