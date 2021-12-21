package plankton.pipeline;

public interface ContainerRuntimeAdapter {

    void pullImage(ContainerConfiguration configuration);

    void buildImage(ContainerConfiguration configuration);

    void createContainer(ContainerConfiguration configuration);

    int startContainerAndGetExitCode(ContainerConfiguration configuration);

    void stopContainer(ContainerConfiguration configuration);

    // boolean isContainerHealthy(ContainerConfiguration configuration);
}
