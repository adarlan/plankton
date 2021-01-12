package me.adarlan.plankton.core;

public interface PipelineConfig {

    String getPipelineId();

    String getComposeFilePath();

    String getWorkspaceDirectoryPath();

    // TODO it is not pipeline config; it is plankton config
    String getMetadataDirectoryPath();
}
