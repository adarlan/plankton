package me.adarlan.plankton.docker;

public interface DockerSandboxConfiguration {

    String id();

    DockerHostConfiguration dockerHostConfiguration();

    String workspaceDirectoryPath();
}
