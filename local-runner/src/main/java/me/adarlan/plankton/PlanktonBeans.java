package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.adarlan.plankton.DockerCompose;
import me.adarlan.plankton.PlanktonConfig;
import me.adarlan.plankton.serializable.PlanktonSerializer;
import me.adarlan.plankton.Pipeline;

@Configuration
public class PlanktonBeans {

    @Value("${plankton.name}")
    private String name;

    @Value("${plankton.file}")
    private String file;

    @Value("${plankton.workspace}")
    private String workspace;

    // @Value("${plankton.environment}")
    // private String environment;

    @Value("${plankton.metadata}")
    private String metadata;

    @Value("${plankton.docker-host}")
    private String dockerHost;

    @Bean
    public PlanktonConfig config() {
        PlanktonConfig config = new PlanktonConfig();
        config.setName(name);
        config.setFile(file);
        config.setWorkspace(workspace);
        // config.setEnvironment(environment);
        config.setMetadata(metadata);
        config.setDockerHost(dockerHost);
        return config;
    }

    @Bean
    public DockerCompose dockerCompose() {
        return new DockerCompose(config());
    }

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(dockerCompose());
    }

    @Bean
    PlanktonSerializer planktonSerializer() {
        return new PlanktonSerializer(pipeline());
    }
}
