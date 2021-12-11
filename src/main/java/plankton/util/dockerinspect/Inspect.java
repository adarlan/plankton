package plankton.util.dockerinspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;

public class Inspect {

    private final String dockerSocket;

    public Inspect(String dockerSocket) {
        this.dockerSocket = dockerSocket;
    }

    public Container getContainer(String containerId) {
        final String command = "docker container inspect " + containerId + " | jq '.[0]'";
        BashScript script = new BashScript();
        script.env("DOCKER_HOST=" + dockerSocket);
        script.command(command);
        String json;
        try {
            json = script.runAndGetOutputString();
        } catch (BashScriptFailedException e) {
            throw new DockerInspectException("Unable to inspect container", e);
        }
        Container container;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            container = objectMapper.readValue(json, Container.class);
        } catch (JsonProcessingException e) {
            throw new DockerInspectException("Unable to map container json", e);
        }
        return container;
    }
}
