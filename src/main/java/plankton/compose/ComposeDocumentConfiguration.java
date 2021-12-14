package plankton.compose;

import java.nio.file.Path;
import java.util.Set;

public interface ComposeDocumentConfiguration {

    Path filePath();

    Path resolvePathsFrom();

    Set<String> targetServices();
}
