package me.adarlan.plankton.compose;

import java.nio.file.Path;

public interface ComposeDocumentConfiguration {

    Path filePath();

    Path resolvePathsFrom(); // TODO optional

    // TODO Set<String> profiles();
}
