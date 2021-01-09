package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.adarlan.plankton.docker.DockerPipelineConfig;
import me.adarlan.plankton.docker.DockerPipelineFactory;
import me.adarlan.plankton.serializable.PlanktonSerializer;
import me.adarlan.plankton.api.Pipeline;

@Configuration
public class PlanktonBeans {

    @Value("${plankton.pipeline.id}")
    private String pipelineId;

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.workspace}")
    private String workspace;

    @Value("${plankton.docker.metadata}")
    private String dockerMetadata;

    @Value("${plankton.docker.host}")
    private String dockerHost;

    @Bean
    public DockerPipelineConfig config() {
        DockerPipelineConfig config = new DockerPipelineConfig();
        config.setPipelineId(pipelineId);
        config.setComposeFile(composeFile);
        config.setWorkspace(workspace);
        config.setMetadata(dockerMetadata);
        config.setDockerHost(dockerHost);
        return config;
    }

    @Bean
    public Pipeline pipeline() {
        DockerPipelineFactory pipelineFactory = new DockerPipelineFactory();
        return pipelineFactory.createPipeline(config());
    }

    @Bean
    PlanktonSerializer planktonSerializer() {
        return new PlanktonSerializer(pipeline());
    }
}
