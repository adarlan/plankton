package me.adarlan.plankton.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeDocumentConfiguration;
import me.adarlan.plankton.PlanktonSetup;

@Component
public class ComposeDocumentBean {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public String projectDirectory() {
                return planktonSetup.getProjectDirectoryPath();
            }

            @Override
            public String filePath() {
                return planktonSetup.getComposeFileSourcePath();
            }
        });
    }
}
