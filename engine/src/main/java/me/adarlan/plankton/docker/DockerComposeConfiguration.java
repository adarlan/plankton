package me.adarlan.plankton.docker;

import me.adarlan.plankton.compose.ComposeConfiguration;

public interface DockerComposeConfiguration extends ComposeConfiguration {

    String dockerHost();
}
