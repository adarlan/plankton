package plankton.perspectives;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class WorkspacePerspective {

    @Getter
    private final String composeFileRelativePath;

    @Getter
    private final String composeDirectoryRelativePath;

    private static final Logger logger = LoggerFactory.getLogger(WorkspacePerspective.class);

    public WorkspacePerspective(@Autowired PlanktonPerspective planktonPerspective) {

        composeFileRelativePath = planktonPerspective.getComposeFilePath()
                .substring(planktonPerspective.getProjectPath().length());

        composeDirectoryRelativePath = planktonPerspective.getComposeDirectoryPath()
                .substring(planktonPerspective.getProjectPath().length());

        logger.debug("WorkspacePerspective.composeFileRelativePath: {}", composeFileRelativePath);
        logger.debug("WorkspacePerspective.composeDirectoryRelativePath: {}", composeDirectoryRelativePath);
    }
}
