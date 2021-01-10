package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.adarlan.plankton.docker.DockerPipelineConfig;
import me.adarlan.plankton.docker.DockerPipelineFactory;
import me.adarlan.plankton.api.Pipeline;

@Configuration
public class PlanktonBeans {

    @Value("${plankton.pipeline.id}")
    private String pipelineId;

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.workspace}")
    private String workspace;

    @Value("${plankton.metadata}")
    private String metadata;

    @Value("${plankton.docker.host}")
    private String dockerHost;

    @Bean
    public DockerPipelineConfig dockerPipelineConfig() {
        return new DockerPipelineConfig() {

            @Override
            public String getPipelineId() {
                return String.valueOf(this.hashCode());
            }

            @Override
            public String getComposeFilePath() {
                return composeFile;
            }

            @Override
            public String getWorkspaceDirectoryPath() {
                return workspace;
            }

            @Override
            public String getMetadataDirectoryPath() {
                return metadata;
            }

            @Override
            public String getDockerHost() {
                return dockerHost;
            }
        };
    }

    @Bean
    public Pipeline pipeline() {
        DockerPipelineFactory pipelineFactory = new DockerPipelineFactory();
        return pipelineFactory.createPipeline(dockerPipelineConfig());
    }

    // @Bean
    // PlanktonSerializer planktonSerializer() {
    // return new PlanktonSerializer(pipeline());
    // }
}
