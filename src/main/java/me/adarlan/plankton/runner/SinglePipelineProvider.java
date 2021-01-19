package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.workflow.Pipeline;

@Component
public class SinglePipelineProvider {

    @Autowired
    private Compose compose;

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(compose);
    }
}
