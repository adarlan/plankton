package me.adarlan.plankton;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.docker.DockerComposeConfiguration;
import me.adarlan.plankton.workflow.Pipeline;

@Configuration
public class PlanktonBeans {

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.workspace}")
    private String workspace;

    @Value("${plankton.metadata}")
    private String metadata;

    @Value("${plankton.docker.host}")
    private String dockerHost;

    @Bean
    public DockerComposeConfiguration dockerComposeConfiguration() {
        return new DockerComposeConfiguration() {

            @Override
            public String projectName() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String filePath() {
                return expandUserHomeTilde(composeFile);
            }

            @Override
            public String projectDirectory() {
                return expandUserHomeTilde(workspace);
            }

            @Override
            public String metadataDirectory() {
                return expandUserHomeTilde(metadata);
            }

            @Override
            public String dockerHost() {
                return dockerHost;
            }

            private String expandUserHomeTilde(String path) {
                if (path.startsWith("~"))
                    return path.replaceFirst("^~", System.getProperty("user.home"));
                else
                    return path;
            }
        };
    }

    @Bean
    public Compose compose() {
        return new DockerCompose(dockerComposeConfiguration());
    }

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(compose());
    }
}
