package me.adarlan.plankton.docker;

public interface DockerHostConfiguration {

    String socketAddress();

    String workspaceDirectoryPath();
}
