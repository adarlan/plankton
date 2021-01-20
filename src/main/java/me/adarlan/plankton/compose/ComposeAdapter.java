package me.adarlan.plankton.compose;

import java.util.function.Consumer;

public interface ComposeAdapter {

    boolean createContainers(String serviceName, int serviceScale, Consumer<String> forEachOutput,
            Consumer<String> forEachError);

    void startContainer(String containerName, Consumer<String> forEachOutput, Consumer<String> forEachError);

    ContainerState getContainerState(String containerName);

    void stopContainer(String containerName);

    boolean killContainer(String containerName);
}
