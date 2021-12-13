package plankton.docker.inspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import plankton.docker.client.DockerClient;

public class DockerInspect {

    private final DockerClient dockerClient;

    public DockerInspect(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public Container getContainer(String containerId) {
        String json = dockerClient.inspectContainerAndGetJson(containerId);
        return getContainerFromJson(json);
    }

    private Container getContainerFromJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(json, Container.class);
        } catch (JsonProcessingException e) {
            throw new DockerInspectException("Unable to get container from JSON", e);
        }
    }
}
