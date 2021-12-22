package plankton.docker.util;

import plankton.util.BashScript;
import plankton.util.BashScriptFailedException;

public class DockerUtils {

    private DockerUtils() {
        super();
    }

    public static String getCurrentContainerId() {
        try {
            String containerId = BashScript
                    .runAndGetOutputString("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
            return (containerId == null || containerId.isBlank()) ? null : containerId;
        } catch (BashScriptFailedException e) {
            return "";
        }
    }
}
