package plankton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class PlanktonConfiguration {

    @Value("${workspace}")
    private String projectDirectory;

    @Value("${file}")
    private String composeFile;

    @Value("${sandbox}")
    private boolean sandboxEnabled;

    @Value("${docker}")
    private String dockerHost;
}
