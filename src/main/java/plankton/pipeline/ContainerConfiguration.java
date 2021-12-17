package plankton.pipeline;

import java.util.function.Consumer;

import lombok.Builder;
import lombok.Getter;
import plankton.compose.ComposeService;

@Builder
@Getter
public class ContainerConfiguration {

    private ComposeService service;
    // The adapter implementation should not know about Compose

    private Integer index;

    private Consumer<String> forEachOutput;
    private Consumer<String> forEachError;
}
