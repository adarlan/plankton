package plankton.spring;

import plankton.util.BashScript;
import plankton.util.BashScriptFailedException;
import plankton.util.DockerUtilsException;

class DockerUtils {

    private DockerUtils() {
        super();
    }

    static String getCurrentContainerId() {
        try {
            String containerId = BashScript
                    .runAndGetOutputString("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
            return (containerId == null || containerId.isBlank()) ? null : containerId;
        } catch (BashScriptFailedException e) {
            throw new DockerUtilsException("Unable to get current container id", e);
        }
    }
}
