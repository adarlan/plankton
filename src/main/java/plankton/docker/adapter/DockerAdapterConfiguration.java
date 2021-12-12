package plankton.docker.adapter;

import plankton.compose.ComposeDocument;
import plankton.docker.daemon.DockerDaemon;

public interface DockerAdapterConfiguration {

    ComposeDocument composeDocument();

    String projectDirectoryPath();

    String projectDirectoryTargetPath();

    DockerDaemon dockerDaemon();

    String namespace();
}
