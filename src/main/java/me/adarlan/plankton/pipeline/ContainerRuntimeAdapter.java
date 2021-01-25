package me.adarlan.plankton.pipeline;

import me.adarlan.plankton.compose.ComposeService;

public interface ContainerRuntimeAdapter {

    void createContainers(ComposeService service);

    int runContainerAndGetExitCode(ComposeService service, int containerIndex);

    void stopContainer(ComposeService service, int containerIndex);
}
