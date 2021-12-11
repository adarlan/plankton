package plankton.perspectives;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.util.dockerinspect.Container;
import plankton.util.dockerinspect.Inspect;
import plankton.util.dockerinspect.Mount;

@Component
public class HostPerspective {

    @Getter
    private final String projectDirectoryPath;

    private static final Logger logger = LoggerFactory.getLogger(HostPerspective.class);

    public HostPerspective(@Autowired PlanktonPerspective planktonPerspective) {
        if (planktonPerspective.isRunningFromHost())
            projectDirectoryPath = planktonPerspective.getProjectPath();
        else {
            projectDirectoryPath = initializeProjectDirectoryPathOnHostWhenRunningFromContainer(planktonPerspective);
        }
        logger.debug("HostPerspective.projectDirectoryPath: {}", projectDirectoryPath);
    }

    private String initializeProjectDirectoryPathOnHostWhenRunningFromContainer(
            PlanktonPerspective planktonPerspective) {
        final String containerId = planktonPerspective.getRunningFromContainerId();
        final String destination = planktonPerspective.getProjectPath();
        Inspect inspect = new Inspect(planktonPerspective.getDockerHostSocketAddress());
        Container container = inspect.getContainer(containerId);
        for (Mount mount : container.getMounts()) {
            if (mount.getDestination().equals(destination)) {
                // TODO what if destination is a super directory of project path?
                return mount.getSource();
            }
        }
        throw new PerspectiveException("Unable to initializeProjectDirectoryPathOnHostWhenRunningFromContainer");
    }
}
