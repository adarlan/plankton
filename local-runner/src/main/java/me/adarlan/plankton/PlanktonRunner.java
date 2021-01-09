package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.docker.PipelineImplementation;

@Component
public class PlanktonRunner implements CommandLineRunner {

    @Autowired
    private PipelineImplementation pipeline;

    @Override
    public void run(String... args) throws Exception {
        pipeline.run();
    }
}