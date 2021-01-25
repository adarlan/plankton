package me.adarlan.plankton.core;

import me.adarlan.plankton.compose.ComposeDocument;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ContainerRuntimeAdapter containerRuntimeAdapter();
}
