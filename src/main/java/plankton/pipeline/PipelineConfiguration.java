package plankton.pipeline;

import plankton.compose.ComposeDocument;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ContainerRuntimeAdapter containerRuntimeAdapter();
}
