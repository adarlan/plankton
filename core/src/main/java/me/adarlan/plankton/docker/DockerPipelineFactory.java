package me.adarlan.plankton.docker;

import me.adarlan.plankton.api.PipelineFactory;

public class DockerPipelineFactory extends PipelineFactory<Pipeline, DockerPipelineConfig> {

    @Override
    public Pipeline createPipeline(DockerPipelineConfig config) {
        DockerCompose dockerCompose = new DockerCompose(config);
        Pipeline pipeline = new Pipeline(dockerCompose);
        return pipeline;
    }
}
