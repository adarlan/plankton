package plankton.docker.util;

import java.util.ArrayList;
import java.util.List;

import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;

public class DockerUtils {

    private DockerUtils() {
        super();
    }

    public static String getCurrentContainerId() {
        List<String> output = new ArrayList<>();
        BashScript script = new BashScript();
        script.command("cat /proc/self/cgroup | grep docker | head -n 1 | cut -d/ -f3");
        script.forEachOutput(output::add);
        try {
            script.run();
        } catch (BashScriptFailedException e) {
            return null;
        }
        if (script.exitCode() != 0
                || script.hasError()
                || output.size() != 1) {
            return null;
        } else {
            String containerId;
            containerId = output.get(0);
            return (containerId == null || containerId.isBlank()) ? null : containerId;
        }
    }
}
