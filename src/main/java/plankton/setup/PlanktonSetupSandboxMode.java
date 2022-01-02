package plankton.setup;

import lombok.Getter;

public class PlanktonSetupSandboxMode {

    @Getter
    private final boolean sandboxEnabled;

    public PlanktonSetupSandboxMode(PlanktonSetup setup) {
        sandboxEnabled = setup.isSandbox();
    }
}
