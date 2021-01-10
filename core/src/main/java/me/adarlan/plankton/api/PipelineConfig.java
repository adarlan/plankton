package me.adarlan.plankton.api;

public interface PipelineConfig {

    default String getPipelineId() {
        return String.valueOf(this.hashCode());
    }

    default String getComposeFilePath() {
        return "plankton.compose.yaml";
    }

    default String getWorkspaceDirectoryPath() {
        return ".";
    }

    // TODO it is not pipeline config; it is plankton config
    default String getMetadataDirectoryPath() {
        return "~/.plankton";
    }
}
