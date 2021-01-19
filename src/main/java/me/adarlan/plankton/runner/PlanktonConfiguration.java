package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

// @Data
// @Configuration
// @ConfigurationProperties(prefix = "plankton")
@Getter
@Component
public class PlanktonConfiguration {

    // TODO private String runnerMode;
    // TODO private boolean dockerEnabled;
    // TODO private String dockerSandboxImage;
    // TODO private String dockerSandboxCache;

    @Value("${plankton.metadata.directory}")
    private String metadataDirectory;

    @Value("${plankton.metadata.directory.underlying}")
    private String metadataDirectoryUnderlying;

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.project.directory}")
    private String projectDirectory;

    @Value("${plankton.docker.host}")
    private String dockerHost;

    @Value("${plankton.docker.sandbox.enabled}")
    private boolean dockerSandboxEnabled;
}
