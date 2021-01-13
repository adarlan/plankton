package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.PipelineFactory;

public class DockerPipelineFactory extends PipelineFactory<PipelineImplementation, DockerPipelineConfig> {

    @Override
    public PipelineImplementation createPipeline(DockerPipelineConfig pipelineConfiguration) {
        DockerComposeConfiguration composeConfiguration = composeConfiguration(pipelineConfiguration);
        DockerCompose dockerCompose = new DockerCompose(composeConfiguration);
        return new PipelineImplementation(dockerCompose);
    }

    private DockerComposeConfiguration composeConfiguration(DockerPipelineConfig pipelineConfig) {
        return new DockerComposeConfiguration() {

            @Override
            public String projectName() {
                return pipelineConfig.getPipelineId();
            }

            @Override
            public String filePath() {
                return pipelineConfig.getComposeFilePath();
            }

            @Override
            public String projectDirectory() {
                return pipelineConfig.getWorkspaceDirectoryPath();
            }

            @Override
            public String metadataDirectory() {
                return pipelineConfig.getMetadataDirectoryPath() + "/" + pipelineConfig.getPipelineId();
            }

            @Override
            public String dockerHost() {
                return pipelineConfig.getDockerHost();
            }
        };
    }
}
