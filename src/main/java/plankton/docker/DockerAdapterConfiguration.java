package plankton.docker;

import plankton.compose.ComposeDocument;

public interface DockerAdapterConfiguration {

    ComposeDocument composeDocument();

    String projectDirectoryPath();

    String projectDirectoryTargetPath();

    DockerDaemon dockerDaemon();

    String namespace();
}
