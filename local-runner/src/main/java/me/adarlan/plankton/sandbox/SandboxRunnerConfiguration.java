package me.adarlan.plankton.sandbox;

public interface SandboxRunnerConfiguration {

    String pipelineId();

    String runnerContainerName();

    String workspaceDirectoryOnHost();

    String workspaceDirectoryOnRunner();

    String composeFileOnRunner();

    String metadataDirectoryOnRunner();
}
