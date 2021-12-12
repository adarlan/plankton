package plankton.core;

import plankton.compose.ComposeDocument;
import plankton.docker.adapter.ContainerRuntimeAdapter;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ContainerRuntimeAdapter containerRuntimeAdapter();
}
