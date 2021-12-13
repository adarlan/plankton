package plankton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import plankton.core.Pipeline;

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
