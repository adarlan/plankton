package me.adarlan.plankton.api;

import lombok.Getter;
import lombok.Setter;

public abstract class PipelineConfig {

    public PipelineConfig() {

    }

    @Getter
    @Setter
    private String pipelineId;

    @Getter
    @Setter
    private String composeFile;

    @Getter
    @Setter
    private String workspace;

    @Getter
    @Setter
    private String metadata;
}