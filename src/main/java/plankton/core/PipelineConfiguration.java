package plankton.core;

import plankton.adapter.ContainerRuntimeAdapter;
import plankton.compose.ComposeDocument;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ContainerRuntimeAdapter containerRuntimeAdapter();
}
