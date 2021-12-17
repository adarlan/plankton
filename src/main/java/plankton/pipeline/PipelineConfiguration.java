package plankton.pipeline;

import java.time.Duration;
import java.util.Set;

import plankton.compose.ComposeDocument;

public interface PipelineConfiguration {

    ComposeDocument composeDocument();

    ContainerRuntimeAdapter containerRuntimeAdapter();

    Set<String> targetJobs();

    Set<String> skipJobs();

    Duration timeoutLimitForJobs();
}
