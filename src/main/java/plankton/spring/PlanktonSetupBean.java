package plankton.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.setup.PlanktonSetup;

@Component
public class PlanktonSetupBean {

    @Autowired
    private PlanktonConfiguration planktonConfiguration;

    @Bean
    public PlanktonSetup planktonSetup() {
        PlanktonSetup planktonSetup = new PlanktonSetup();
        planktonSetup.setDocker(planktonConfiguration.getDocker());
        planktonSetup.setFile(planktonConfiguration.getFile());
        planktonSetup.setSandbox(planktonConfiguration.isSandbox());
        planktonSetup.setSkip(planktonConfiguration.getSkip());
        planktonSetup.setTarget(planktonConfiguration.getTarget());
        planktonSetup.setWorkspace(planktonConfiguration.getWorkspace());
        planktonSetup.setup();
        return planktonSetup;
    }
}
