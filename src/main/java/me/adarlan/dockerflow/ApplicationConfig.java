package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Value("${dockerflow.id}")
    private String id;

    @Value("${dockerflow.file}")
    private String file;

    @Value("${dockerflow.workspace}")
    private String workspace;

    @Bean
    public String id() {
        return id;
    }

    @Bean
    public String file() {
        return file;
    }

    @Bean
    public String workspace() {
        return workspace;
    }
}