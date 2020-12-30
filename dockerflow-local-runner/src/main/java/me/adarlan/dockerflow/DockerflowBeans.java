package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerflowBeans {

    @Value("${dockerflow.name}")
    private String name;

    @Value("${dockerflow.file}")
    private String file;

    @Value("${dockerflow.workspace}")
    private String workspace;

    //@Value("${dockerflow.environment}")
    //private String environment;

    @Value("${dockerflow.metadata}")
    private String metadata;

    @Bean
    public DockerflowConfig config() {
        DockerflowConfig config = new DockerflowConfig();
        config.setName(name);
        config.setFile(file);
        config.setWorkspace(workspace);
        //config.setEnvironment(environment);
        config.setMetadata(metadata);
        return config;
    }

    @Bean
    public DockerCompose dockerCompose() {
        return new DockerCompose(config());
    }

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(dockerCompose());
    }
}
