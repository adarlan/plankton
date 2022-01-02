package plankton.setup;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;

public class PlanktonSetupRunningFrom {

    @Getter
    private final boolean runningFromHost;

    @Getter
    private final String runningFromContainerId;

    public PlanktonSetupRunningFrom() {
        runningFromContainerId = getCurrentContainerId();
        runningFromHost = (runningFromContainerId == null);
    }

    private String getCurrentContainerId() {
        List<String> output = new ArrayList<>();
        BashScript script = new BashScript();
        script.command("FOO=$(cat /proc/self/cgroup | grep docker | head -n 1)");
        script.command("[ -z \"$FOO\" ] || echo ${FOO##*/docker/}");
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
