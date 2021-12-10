package plankton.docker;

public class DockerHost implements DockerDaemon {

    private final String socketAddress;

    public DockerHost(DockerHostConfiguration configuration) {
        this.socketAddress = configuration.socketAddress();
    }

    @Override
    public String toString() {
        return DockerHost.class.getSimpleName() + "(" + socketAddress + ")";
    }

    @Override
    public String socketAddress() {
        return socketAddress;
    }
}
