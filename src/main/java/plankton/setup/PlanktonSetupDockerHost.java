package plankton.setup;

import lombok.Getter;
import plankton.docker.client.DockerClient;
import plankton.docker.daemon.DockerHostDaemon;

public class PlanktonSetupDockerHost {

    @Getter
    private final String dockerHostSocketAddress;

    @Getter
    private final DockerHostDaemon dockerHostDaemon;

    @Getter
    private final DockerClient dockerHostClient;

    public PlanktonSetupDockerHost(PlanktonSetup setup) {
        dockerHostSocketAddress = dockerHostSocketAddress(setup.getDocker());
        dockerHostDaemon = new DockerHostDaemon(() -> dockerHostSocketAddress);
        dockerHostClient = new DockerClient(dockerHostDaemon);
    }

    private String dockerHostSocketAddress(String s) {
        return (s == null || s.isBlank())
                ? "unix:///var/run/docker.sock"
                : s;
    }
}
