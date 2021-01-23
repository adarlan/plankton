package me.adarlan.plankton.docker;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NoArgsConstructor;
import lombok.ToString;
import me.adarlan.plankton.compose.ContainerState;

@NoArgsConstructor
@ToString
public class DockerContainerState implements ContainerState {

    @JsonProperty("Status")
    String status;

    @JsonProperty("Running")
    boolean running;

    @JsonProperty("Paused")
    boolean paused;

    @JsonProperty("Restarting")
    boolean restarting;

    @JsonProperty("OOMKilled")
    boolean oomKilled;

    @JsonProperty("Dead")
    boolean dead;

    @JsonProperty("Pid")
    int pid;

    @JsonProperty("ExitCode")
    int exitCode;

    @JsonProperty("Error")
    String error;

    @JsonProperty("StartedAt")
    String startedAt;

    @JsonProperty("FinishedAt")
    String finishedAt;

    @Override
    public boolean running() {
        return status.equals("running");
    }

    @Override
    public boolean exited() {
        return status.equals("exited");
    }

    @Override
    public Instant initialInstant() {
        return parseInstant(startedAt);
    }

    @Override
    public Instant finalInstant() {
        return parseInstant(finishedAt);
    }

    @Override
    public Integer exitCode() {
        return exitCode;
    }

    private Instant parseInstant(String text) {
        if (text.equals("0001-01-01T00:00:00Z"))
            return null;
        else
            return Instant.parse(text);
    }
}
