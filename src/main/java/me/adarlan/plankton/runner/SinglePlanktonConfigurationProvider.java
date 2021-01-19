package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.bash.BashScript;

@Component
@ConditionalOnExpression("'${plankton.runner.mode}'=='single-pipeline'")
public class SinglePlanktonConfigurationProvider {

    @Value("${plankton.compose.file}")
    private String composeFilePath;

    @Value("${plankton.project.directory}")
    private String projectDirectoryPath;

    @Bean
    public PlanktonConfiguration planktonConfiguration() {
        return new PlanktonConfiguration(composeFilePath, workspaceDirectoryPath -> {
            BashScript script = new BashScript("make_workspace");
            script.command("cp -R " + projectDirectoryPath + "/. " + workspaceDirectoryPath + "/");
            script.runSuccessfully();
        });
    }
}
