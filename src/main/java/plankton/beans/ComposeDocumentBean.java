package plankton.beans;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import plankton.PlanktonConfiguration;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeDocumentConfiguration;
import plankton.perspectives.PlanktonPerspective;
import plankton.perspectives.SandboxPerspective;

@Component
public class ComposeDocumentBean {

    @Autowired
    private PlanktonPerspective planktonPerspective;

    @Autowired
    private SandboxPerspective sandboxPerspective;

    @Autowired
    private PlanktonConfiguration planktonConfiguration;

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(planktonPerspective.getComposeFilePath());
            }

            @Override
            public Path resolvePathsFrom() {
                return Paths.get(sandboxPerspective.getComposeDirectoryTargetPath());
            }

            @Override
            public String targetServices() {
                return planktonConfiguration.getTargetJobs();
            }
        });
    }
}
