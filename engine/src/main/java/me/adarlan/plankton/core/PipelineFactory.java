package me.adarlan.plankton.core;

public abstract class PipelineFactory<P extends Pipeline, C extends PipelineConfig> {

    public abstract P createPipeline(C config);
}
