package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.api.Pipeline;

@Component
public class PlanktonRunner implements CommandLineRunner {

    @Autowired
    private Pipeline pipeline;

    @Override
    public void run(String... args) throws Exception {
        pipeline.run();
    }
}