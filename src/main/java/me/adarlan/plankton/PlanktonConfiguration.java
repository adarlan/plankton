package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class PlanktonConfiguration {

    @Value("${plankton.project.directory}")
    private String projectDirectory;

    @Value("${plankton.project.directory.underlying}")
    private String projectDirectoryOnHost;

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.docker.sandbox.enabled}")
    private boolean sandboxEnabled;

    @Value("${plankton.docker.host}")
    private String dockerHost;
}
