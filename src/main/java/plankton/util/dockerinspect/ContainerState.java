package plankton.util.dockerinspect;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
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

    public boolean running() {
        return status.equals("running");
    }

    public boolean exited() {
        return status.equals("exited");
    }

    public Instant initialInstant() {
        return parseInstant(startedAt);
    }

    public Instant finalInstant() {
        return parseInstant(finishedAt);
    }

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
