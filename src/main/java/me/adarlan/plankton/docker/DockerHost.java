package me.adarlan.plankton.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;

@ToString(of = { "socketAddress" })
public class DockerHost implements DockerDaemon {

    private final String socketAddress;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DockerHost(DockerHostConfiguration configuration) {
        logger.info("Loading DockerHost");
        this.socketAddress = configuration.socketAddress();
        logger.info("socketAddress={}", socketAddress);
    }

    @Override
    public String getSocketAddress() {
        return socketAddress;
    }
}
