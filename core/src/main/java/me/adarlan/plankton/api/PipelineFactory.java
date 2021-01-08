package me.adarlan.plankton.api;

public abstract class PipelineFactory {

    public abstract Pipeline createPipeline(PipelineConfig config);
}
