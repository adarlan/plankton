package plankton.docker.adapter;

import plankton.compose.ComposeDocument;
import plankton.docker.daemon.DockerDaemon;

public interface DockerAdapterConfiguration {

    ComposeDocument composeDocument();

    DockerDaemon dockerDaemon();

    String namespace();

    String workspacePathFromRunnerPerspective();

    String workspacePathFromAdapterPerspective();
}
