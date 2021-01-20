package me.adarlan.plankton.compose;

import java.util.function.Consumer;

public interface ComposeAdapter {

    boolean createContainers(String serviceName, int serviceScale, Consumer<String> forEachOutput,
            Consumer<String> forEachError);
    // TODO throws ContainersCreationFailedException

    boolean runContainer(String containerName, Consumer<String> forEachOutput, Consumer<String> forEachError);
    // TODO throws ContainerRunningFailedException

    ContainerState getContainerState(String containerName);

    boolean stopContainer(String containerName);
    // TODO ContainerStoppingFailedException

    void disconnect();
}
