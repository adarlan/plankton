package me.adarlan.dockerflow;

import lombok.Data;

@Data
public class DockerflowConfig {

    String name;

    String file;

    String workspace;

    // String environment;

    String metadata;

    String dockerHost;
}