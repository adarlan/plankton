package me.adarlan.plankton.compose;

import java.nio.file.Path;

public interface ComposeDocumentConfiguration {

    Path filePath();

    Path resolvePathsFrom();

    // TODO Set<String> profiles();
}
