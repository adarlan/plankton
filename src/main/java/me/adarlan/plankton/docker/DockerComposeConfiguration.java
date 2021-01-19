package me.adarlan.plankton.docker;

import me.adarlan.plankton.compose.ComposeDocument;

public interface DockerComposeConfiguration {

    DockerDaemon dockerDaemon();

    ComposeDocument composeDocument();

    String containerStateDirectoryPath();
}
