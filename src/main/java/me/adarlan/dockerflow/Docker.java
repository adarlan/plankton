package me.adarlan.dockerflow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import me.adarlan.dockerflow.bash.BashScript;

@Component
public class Docker {

    public boolean runContainer(Job job, String containerName) throws InterruptedException {
        final List<String> logs = job.logs;
        final String scriptName = "run-container_" + containerName;
        final BashScript script = new BashScript(scriptName);
        script.command("docker container start --attach " + containerName);
        script.forEachOutputAndError(line -> {
            synchronized (logs) {
                logs.add(line);
                Logger.follow(() -> "          " + containerName + " >> " + line);
            }
        });
        script.run();
        return script.getExitCode() == 0;
    }

    public ContainerState containerState(String containerName) throws InterruptedException {
        final List<String> scriptOutput = new ArrayList<>();
        final String scriptName = "container-state_" + containerName;
        final BashScript script = new BashScript(scriptName);
        script.command("docker container inspect " + containerName + " | jq --compact-output '.[] | .State'");
        script.forEachOutput(line -> {
            scriptOutput.add(line);
            Logger.debug(() -> scriptName + " >> " + line);
        });
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        script.run();
        final String json = scriptOutput.stream().collect(Collectors.joining());
        final ContainerState state = parseContainerState(json);
        Logger.debug(state::toString);
        return state;
    }

    public boolean killContainer(String containerName) throws InterruptedException {
        String scriptName = "kill-container_" + containerName;
        BashScript script = new BashScript(scriptName);
        script.command("docker container kill " + containerName);
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        script.run();
        return script.getExitCode() == 0;
    }

    private ContainerState parseContainerState(String json) {
        try {
            return new ObjectMapper().readValue(json, ContainerState.class);
        } catch (JsonProcessingException e) {
            throw new DockerflowException("Unable to parse container state JSON: " + json, e);
        }
    }
}