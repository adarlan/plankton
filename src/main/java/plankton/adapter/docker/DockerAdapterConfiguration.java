package plankton.adapter.docker;

import plankton.compose.ComposeDocument;
import plankton.docker.DockerDaemon;

public interface DockerAdapterConfiguration {

    ComposeDocument composeDocument();

    String projectDirectoryPath();

    String projectDirectoryTargetPath();

    DockerDaemon dockerDaemon();

    String namespace();
}
