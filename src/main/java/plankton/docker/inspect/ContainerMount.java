package plankton.docker.inspect;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@ToString
@Getter
public class ContainerMount {

    @JsonProperty("Type")
    String type;

    @JsonProperty("Source")
    String source;

    @JsonProperty("Destination")
    String destination;

    @JsonProperty("Mode")
    String mode;

    @JsonProperty("RW")
    boolean rw;

    @JsonProperty("Propagation")
    String propagation;
}
