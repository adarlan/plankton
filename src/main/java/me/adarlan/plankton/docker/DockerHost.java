package me.adarlan.plankton.docker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.ToString;

@ToString(of = { "socketAddress" })
public class DockerHost implements DockerDaemon {

    private final String socketAddress;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String LOADING = "Loading " + DockerHost.class.getSimpleName() + " ... ";

    public DockerHost(DockerHostConfiguration configuration) {
        logger.info(LOADING);
        this.socketAddress = configuration.socketAddress();
        logger.info("{}socketAddress={}", LOADING, socketAddress);
    }

    @Override
    public String getSocketAddress() {
        return socketAddress;
    }

    @Override
    public void disconnect() {
        /* ignore */
    }
}
