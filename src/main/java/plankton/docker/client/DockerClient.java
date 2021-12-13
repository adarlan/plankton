package plankton.docker.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;
import plankton.docker.daemon.DockerDaemon;

public class DockerClient {

    private final DockerDaemon daemon;

    private final Logger logger = LoggerFactory.getLogger(DockerClient.class);

    public DockerClient(DockerDaemon daemon1) {
        daemon = daemon1;
    }

    @SuppressWarnings("unchecked")
    public abstract class Command<T> {
        private final String cmd;
        private final List<String> options = new ArrayList<>();
        private Consumer<String> forEachOutput;
        private Consumer<String> forEachError;
        private boolean allowFailure = false;

        private Command(String cmd) {
            this.cmd = cmd;
        }

        public T option(String o) {
            options.add(o);
            return (T) this;
        }

        public T forEachOutput(Consumer<String> f) {
            forEachOutput = f;
            return (T) this;
        }

        public T forEachError(Consumer<String> f) {
            forEachError = f;
            return (T) this;
        }

        public T allowFailure() {
            allowFailure = true;
            return (T) this;
        }

        int apply(String args) {
            String opts = String.join(" ", options);
            String line = cmd + " " + opts + " " + args;
            BashScript script = createBashScript();
            script.command(line);
            script.forEachOutput(forEachOutput);
            script.forEachError(forEachError);
            try {
                script.run();
            } catch (BashScriptFailedException e) {
                if (!allowFailure)
                    throw new DockerClientException("Command failed: " + line, e);
            }
            return script.exitCode();
            // TODO validate command with regex to avoid script injection
        }
    }

    public class ImagePuller extends Command<ImagePuller> {

        private ImagePuller() {
            super("docker image pull");
        }

        public void pullImage(String tag) {
            apply(tag);
        }

        // TODO credential_spec ???
    }

    public class ImageBuilder extends Command<ImageBuilder> {

        private String context;

        private ImageBuilder() {
            super("docker image build");
        }

        public ImageBuilder context(String c) {
            context = c;
            return this;
        }

        public void buildImage() {
            apply(context);
        }
    }

    public class ContainerCreator extends Command<ContainerCreator> {

        private String image = "";
        private String args = "";

        public ContainerCreator() {
            super("docker container create");
        }

        public ContainerCreator image(String x) {
            image = x;
            return this;
        }

        public ContainerCreator args(String x) {
            args = x;
            return this;
        }

        public void createContainer() {
            apply(image + " " + args);
        }
    }

    public class ContainerStarter extends Command<ContainerStarter> {

        private ContainerStarter() {
            super("docker container start");
        }

        public int startAndGetExitCode(String containerName) {
            allowFailure();
            return apply("--attach " + containerName);
        }

        public void startDetached(String containerName) {
            apply("--detach " + containerName);
        }
    }

    public class ContainerStopper extends Command<ContainerStopper> {

        private ContainerStopper() {
            super("docker container stop");
        }

        public void stopContainer(String name) {
            apply(name);
        }
    }

    public class ContainerKiller extends Command<ContainerKiller> {

        private ContainerKiller() {
            super("docker container kill");
        }

        public void killContainer(String name) {
            apply(name);
        }
    }

    public class NetworkCreator extends Command<NetworkCreator> {

        private NetworkCreator() {
            super("docker network create");
        }

        public void createAttachableNetwork(String name) {
            apply("--attachable " + name);
        }
    }

    public ImagePuller imagePuller() {
        return new ImagePuller();
    }

    public ImageBuilder imageBuilder() {
        return new ImageBuilder();
    }

    public ContainerCreator containerCreator() {
        return new ContainerCreator();
    }

    public ContainerStarter containerStarter() {
        return new ContainerStarter();
    }

    public ContainerStopper containerStopper() {
        return new ContainerStopper();
    }

    public ContainerKiller containerKiller() {
        return new ContainerKiller();
    }

    public NetworkCreator networkCreator() {
        return new NetworkCreator();
    }

    // public void createAttachableNetwork(String name) {
    // BashScript script = createBashScript();
    // script.command("docker network create --attachable " + name);
    // try {
    // script.run();
    // } catch (BashScriptFailedException e) {
    // throw new DockerClientException("Unable to create network: " + name, e);
    // }
    // }

    public boolean imageExists(String imageTag) {
        logger.debug("Checking if image exists: {}", imageTag);
        List<String> list = new ArrayList<>();
        BashScript script = createBashScript();
        script.command("docker images -q " + imageTag);
        script.forEachOutput(list::add);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            throw new DockerClientException("Unable to check if image exists: " + imageTag, e);
        }
        if (list.isEmpty()) {
            logger.debug("Image does not exist: {}", imageTag);
            return false;
        } else {
            logger.debug("Image exists: {}", imageTag);
            return true;
        }
    }

    public String inspectContainerAndGetJson(String containerId) {
        BashScript script = createBashScript();
        script.command("docker container inspect " + containerId + " | jq '.[0]'");
        try {
            return script.runAndGetOutputString();
        } catch (BashScriptFailedException e) {
            throw new DockerClientException("Unable to inspect container: " + containerId, e);
        }
    }

    private BashScript createBashScript() {
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + daemon.socketAddress());
        return script;
    }
}
