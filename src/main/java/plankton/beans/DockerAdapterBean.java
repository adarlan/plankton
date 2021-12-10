package plankton.beans;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.compose.ComposeDocument;
import plankton.core.ContainerRuntimeAdapter;
import plankton.docker.DockerAdapter;
import plankton.docker.DockerAdapterConfiguration;
import plankton.docker.DockerDaemon;
import plankton.PlanktonSetup;

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
                int min = 1;
                int max = 1000000000;
                double r = (Math.random() * ((max - min) + 1)) + min;
                return String.valueOf(Instant.now().getEpochSecond()) + "_" + String.valueOf(r).trim();
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
