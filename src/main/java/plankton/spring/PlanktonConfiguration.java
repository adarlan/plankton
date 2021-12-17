package plankton.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class PlanktonConfiguration {

    @Value("${workspace}")
    private String workspace;

    @Value("${file}")
    private String file;

    @Value("${sandbox}")
    private boolean sandbox;

    @Value("${docker}")
    private String docker;

    @Value("${target}")
    private String target;

    @Value("${skip}")
    private String skip;
}
