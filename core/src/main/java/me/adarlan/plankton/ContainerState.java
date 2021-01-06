package me.adarlan.plankton;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContainerState {

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
}
