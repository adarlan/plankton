package plankton.util.dockercontainerinspect;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class Container {

    @JsonProperty("Mounts")
    List<Mount> mounts;
}
