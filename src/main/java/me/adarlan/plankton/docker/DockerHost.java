package me.adarlan.plankton.docker;

public class DockerHost implements DockerDaemon {

    private final String socketAddress;

    public DockerHost(DockerHostConfiguration configuration) {
        this.socketAddress = configuration.socketAddress();
    }

    @Override
    public String getSocketAddress() {
        return socketAddress;
    }
}
