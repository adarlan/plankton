package plankton.docker.adapter;

import plankton.docker.daemon.DockerDaemon;

public interface DockerAdapterConfiguration {

    DockerDaemon dockerDaemon();

    String namespace();

    String workspacePathFromRunnerPerspective();

    String workspacePathFromAdapterPerspective();
}
