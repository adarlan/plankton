package me.adarlan.plankton.docker;

import me.adarlan.plankton.core.PipelineConfig;

public interface DockerPipelineConfig extends PipelineConfig {

    default String getDockerHost() {
        return "unix:///var/run/docker.sock";
    }
}