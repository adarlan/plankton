package plankton.docker.api;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;
import plankton.docker.daemon.DockerDaemon;

public class DockerApi {

    private final DockerDaemon daemon;

    private final Logger logger = LoggerFactory.getLogger(DockerApi.class);

    public DockerApi(DockerDaemon daemon1) {
        daemon = daemon1;
    }

    public void createAttachableNetwork(String name) {
        BashScript script = createBashScript();
        script.command("docker network create --attachable " + name);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerApiException("Unable to create network: " + name, e);
        }
    }

    public boolean imageExists(String imageTag) {
        logger.debug("Checking if image exists: {}", imageTag);
        List<String> list = new ArrayList<>();
        BashScript script = createBashScript();
        script.command("docker images -q " + imageTag);
        script.forEachOutput(list::add);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerApiException("Unable to check if image exists: " + imageTag, e);
        }
        if (list.isEmpty()) {
            logger.debug("Image don't exists: {}", imageTag);
            return false;
        } else {
            logger.debug("Image exists: {}", imageTag);
            return true;
        }
    }

    private BashScript createBashScript() {
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + daemon.socketAddress());
        return script;
    }
}
