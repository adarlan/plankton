package plankton.docker;

public interface DockerSandboxConfiguration {

    String id();

    String dockerHostSocketAddress();

    String underlyingWorkspaceDirectoryPath();

    String workspaceDirectoryPath();

    boolean runningFromHost();

    String runningFromContainerId();
}
