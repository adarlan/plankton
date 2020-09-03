package me.adarlan.dockerflow.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerflowBeans {

    @Bean
    @Autowired
    public List<String> environmentVariables(DockerflowConfig dockerflowConfig) {
        List<String> environmentVariables = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(dockerflowConfig.getEnvironment());
                Scanner scanner = new Scanner(fileInputStream);) {
            scanner.forEachRemaining(environmentVariables::add);
        } catch (FileNotFoundException e) {
            // TODO ignore?
        } catch (IOException e) {
            throw new DockerflowConfigException(e);
        }
        return environmentVariables;
    }
}