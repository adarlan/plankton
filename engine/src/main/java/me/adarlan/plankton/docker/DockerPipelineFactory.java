package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.PipelineFactory;

public class DockerPipelineFactory extends PipelineFactory<PipelineImplementation, DockerPipelineConfig> {

    @Override
    public PipelineImplementation createPipeline(DockerPipelineConfig config) {
        DockerCompose dockerCompose = new DockerCompose(config);
        PipelineImplementation pipeline = new PipelineImplementation(dockerCompose);
        return pipeline;
    }
}
