package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.core.Pipeline;

@Component
public class PipelineRunner implements CommandLineRunner {

	@Autowired
	private Pipeline pipeline;

	@Override
	public void run(String... args) throws Exception {
		pipeline.start();
	}
}
