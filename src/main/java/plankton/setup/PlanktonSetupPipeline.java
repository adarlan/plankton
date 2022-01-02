package plankton.setup;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import plankton.compose.ComposeDocument;
import plankton.pipeline.ContainerRuntimeAdapter;
import plankton.pipeline.Pipeline;
import plankton.pipeline.PipelineConfiguration;
import plankton.pipeline.PipelineInitializer;

public class PlanktonSetupPipeline {

    @Getter
    private final Pipeline pipeline;

    public PlanktonSetupPipeline(
            PlanktonSetup setup,
            PlanktonSetupDockerAdapter dockerAdapter,
            PlanktonSetupComposeDocument composeDocument) {

        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration() {

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument.getComposeDocument();
            }

            @Override
            public ContainerRuntimeAdapter containerRuntimeAdapter() {
                return dockerAdapter.getDockerAdapter();
            }

            @Override
            public Set<String> targetJobs() {
                return new HashSet<>(split(setup.getTarget()));
            }

            @Override
            public Duration timeoutLimitForJobs() {
                return Duration.of(10L, ChronoUnit.MINUTES);
            }

            @Override
            public Set<String> skipJobs() {
                return new HashSet<>(split(setup.getSkip()));
            }
        };
        PipelineInitializer pipelineInitializer = new PipelineInitializer(pipelineConfiguration);
        pipeline = pipelineInitializer.pipeline();
    }

    private List<String> split(String s) {
        return (s == null || s.isBlank())
                ? new ArrayList<>()
                : Arrays.asList(s.split(","));
    }
}
