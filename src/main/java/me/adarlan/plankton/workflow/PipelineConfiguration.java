package me.adarlan.plankton.workflow;

import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.compose.ComposeDocument;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ComposeAdapter composeAdapter();
}
