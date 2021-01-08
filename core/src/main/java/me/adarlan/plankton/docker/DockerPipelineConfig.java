package me.adarlan.plankton.docker;

import lombok.Getter;
import lombok.Setter;
import me.adarlan.plankton.api.PipelineConfig;

public class DockerPipelineConfig extends PipelineConfig {

    public DockerPipelineConfig() {
        super();
    }

    @Getter
    @Setter
    private String metadata;

    @Getter
    @Setter
    private String dockerHost;
}