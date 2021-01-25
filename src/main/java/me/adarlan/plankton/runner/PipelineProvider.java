package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.core.ContainerRuntimeAdapter;
import me.adarlan.plankton.core.Pipeline;
import me.adarlan.plankton.core.PipelineConfiguration;

@Component
public class PipelineProvider {

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
