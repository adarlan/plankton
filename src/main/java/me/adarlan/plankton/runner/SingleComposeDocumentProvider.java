package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeDocumentConfiguration;

@Component
@ConditionalOnExpression("'${plankton.runner.mode}'=='single-pipeline'")
public class SingleComposeDocumentProvider {

    @Autowired
    private PlanktonConfiguration planktonConfiguration;

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public String projectName() {
                return planktonConfiguration.getId();
            }

            @Override
            public String filePath() {
                return planktonConfiguration.getComposeFilePath();
            }

            @Override
            public String projectDirectory() {
                return planktonConfiguration.getWorkspaceDirectoryPath();
            }
        });
    }
}
