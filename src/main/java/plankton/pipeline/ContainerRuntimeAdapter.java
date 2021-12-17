package plankton.pipeline;

public interface ContainerRuntimeAdapter {

    void pullImage(ContainerConfiguration configuration);

    void buildImage(ContainerConfiguration configuration);

    void createContainers(ContainerConfiguration configuration);

    int startContainerAndGetExitCode(ContainerConfiguration configuration);

    void stopContainer(ContainerConfiguration configuration);
}
