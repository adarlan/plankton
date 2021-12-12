package plankton.beans;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.compose.ComposeDocument;
import plankton.docker.adapter.ContainerRuntimeAdapter;
import plankton.docker.adapter.DockerAdapter;
import plankton.docker.adapter.DockerAdapterConfiguration;
import plankton.docker.daemon.DockerDaemon;
import plankton.perspectives.PlanktonPerspective;
import plankton.perspectives.SandboxPerspective;

@Component
public class DockerAdapterBean {

    @Autowired
    private PlanktonPerspective planktonPerspective;

    @Autowired
    private SandboxPerspective sandboxPerspective;

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
                return planktonPerspective.getProjectPath();
            }

            @Override
            public String projectDirectoryTargetPath() {
                return sandboxPerspective.getProjectDirectoryTargetPath();
            }
        });
    }
}
