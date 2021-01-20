package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.workflow.Pipeline;

@Component
public class PipelineProvider {

    @Autowired
    private ComposeAdapter composeAdapter;

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(composeAdapter);
    }
}
