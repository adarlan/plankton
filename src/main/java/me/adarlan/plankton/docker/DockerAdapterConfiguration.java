package me.adarlan.plankton.docker;

import me.adarlan.plankton.compose.ComposeDocument;

public interface DockerAdapterConfiguration {

    ComposeDocument composeDocument();

    String projectDirectoryPath();

    String projectDirectoryTargetPath();

    DockerDaemon dockerDaemon();

    String namespace();
}
