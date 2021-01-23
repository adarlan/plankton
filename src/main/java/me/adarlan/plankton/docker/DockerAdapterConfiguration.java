package me.adarlan.plankton.docker;

import me.adarlan.plankton.compose.ComposeDocument;

public interface DockerAdapterConfiguration {

    DockerDaemon dockerDaemon();

    ComposeDocument composeDocument();
}
