package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Value("${pipeline.id}")
    private String pipelineId;

    @Bean
    public String pipelineId() {
        return pipelineId;
    }
}