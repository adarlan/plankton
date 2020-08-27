package me.adarlan.dockerflow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "dockerflow")
@Data
public class ApplicationConfig {

    private String name;

    private String file;

    private String workspace;

    private String environment;

    private String metadata;
}