package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.pipeline.Pipeline;
import me.adarlan.plankton.pipeline.PipelineConfiguration;

@Component
public class PipelineProvider {

    @Autowired
    private ComposeDocument composeDocument;

    @Autowired
    private ComposeAdapter composeAdapter;

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(new PipelineConfiguration() {

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public ComposeAdapter composeAdapter() {
                return composeAdapter;
            }
        });
    }
}
