package me.adarlan.plankton.api;

import lombok.Data;

@Data
public abstract class PipelineConfig {

    private String pipelineId;

    private String composeFile;

    // private String environment;
}