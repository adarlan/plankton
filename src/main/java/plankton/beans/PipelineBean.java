package plankton.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.compose.ComposeDocument;
import plankton.core.Pipeline;
import plankton.core.PipelineConfiguration;
import plankton.docker.adapter.ContainerRuntimeAdapter;

@Component
public class PipelineBean {

    @Autowired
    private ComposeDocument composeDocument;

    @Autowired
    private ContainerRuntimeAdapter containerRuntimeAdapter;

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(new PipelineConfiguration() {

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public ContainerRuntimeAdapter containerRuntimeAdapter() {
                return containerRuntimeAdapter;
            }
        });
    }
}
