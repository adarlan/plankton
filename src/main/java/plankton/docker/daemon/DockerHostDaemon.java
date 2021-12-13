package plankton.docker.daemon;

public class DockerHostDaemon implements DockerDaemon {

    private final String socketAddress;

    public DockerHostDaemon(DockerHostConfiguration configuration) {
        this.socketAddress = configuration.socketAddress();
    }

    @Override
    public String toString() {
        return DockerHostDaemon.class.getSimpleName() + "(" + socketAddress + ")";
    }

    @Override
    public String socketAddress() {
        return socketAddress;
    }
}
