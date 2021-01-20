package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeDocumentConfiguration;

@Component
public class ComposeDocumentProvider {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public String projectName() {
                return planktonSetup.getPipelineId();
            }

            @Override
            public String filePath() {
                return planktonSetup.getComposeFilePath();
            }

            @Override
            public String projectDirectory() {
                return planktonSetup.getWorkspaceDirectoryPath();
            }
        });
    }
}
