package plankton.docker.daemon;

public interface DockerSandboxConfiguration {

    String namespace();

    String dockerHostSocketAddress();

    String underlyingWorkspaceDirectoryPath();

    String workspaceDirectoryPath();

    boolean runningFromHost();

    String runningFromContainerId();
}
