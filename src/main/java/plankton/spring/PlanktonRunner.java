package plankton.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import plankton.pipeline.Pipeline;
import plankton.setup.PlanktonSetup;

@Component
public class PlanktonRunner implements CommandLineRunner {

	@Autowired
	private PlanktonSetup planktonSetup;

	@Override
	public void run(String... args) throws Exception {
		Pipeline pipeline = planktonSetup.getPipeline();
		pipeline.start();
	}
}
