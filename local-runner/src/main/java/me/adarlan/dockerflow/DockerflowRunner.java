package me.adarlan.dockerflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DockerflowRunner implements CommandLineRunner {

    @Autowired
    private Pipeline pipeline;

    @Override
    public void run(String... args) throws Exception {
        pipeline.run();
    }
}