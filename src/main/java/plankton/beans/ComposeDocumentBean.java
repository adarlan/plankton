package plankton.beans;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.compose.ComposeDocument;
import plankton.compose.ComposeDocumentConfiguration;
import plankton.PlanktonSetup;

@Component
public class ComposeDocumentBean {

    @Autowired
    private PlanktonSetup planktonSetup;

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(planktonSetup.getComposeFileSourcePath());
            }

            @Override
            public Path resolvePathsFrom() {
                return Paths.get(planktonSetup.getComposeDirectoryTargetPath());
            }
        });
    }
}
