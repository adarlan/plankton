package me.adarlan.plankton.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.workflow.Pipeline;

@Component
@ConditionalOnExpression("'${plankton.runner.mode}'=='single-pipeline'")
public class SinglePipelineProvider {

    @Autowired
    private Compose compose;

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(compose);
    }
}
