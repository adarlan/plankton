package plankton.setup;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeDocumentConfiguration;
import plankton.compose.ComposeInitializer;

public class PlanktonSetupComposeDocument {

    @Getter
    private final ComposeDocument composeDocument;

    public PlanktonSetupComposeDocument(PlanktonSetupPaths paths) {
        ComposeInitializer composeInitializer = new ComposeInitializer(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(paths.getComposeFilePathFromPlanktonPerspective());
            }

            @Override
            public Path resolvePathsFrom() {
                return Paths.get(paths.getComposeFilePathFromAdapterPerspective()).getParent();
            }
        });
        composeDocument = composeInitializer.composeDocument();
    }
}
