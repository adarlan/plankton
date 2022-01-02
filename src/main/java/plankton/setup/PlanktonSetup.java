package plankton.setup;

import lombok.Getter;
import lombok.Setter;
import plankton.pipeline.Pipeline;

public class PlanktonSetup {

    @Getter
    @Setter
    private String workspace;

    @Getter
    @Setter
    private String file;

    @Getter
    @Setter
    private boolean sandbox;

    @Getter
    @Setter
    private String docker;

    @Getter
    @Setter
    private String target;

    @Getter
    @Setter
    private String skip;

    private PlanktonSetupNamespace namespace;
    private PlanktonSetupDockerHost dockerHost;
    private PlanktonSetupRunningFrom runningFrom;
    private PlanktonSetupSandboxMode sandboxMode;
    private PlanktonSetupPaths paths;
    private PlanktonSetupDockerAdapter dockerAdapter;
    private PlanktonSetupComposeDocument composeDocument;
    private PlanktonSetupPipeline setupPipeline;

    public PlanktonSetup() {
    }

    public void setup() {
        namespace = new PlanktonSetupNamespace();
        dockerHost = new PlanktonSetupDockerHost(this);
        runningFrom = new PlanktonSetupRunningFrom();
        sandboxMode = new PlanktonSetupSandboxMode(this);
        paths = new PlanktonSetupPaths(
                this,
                runningFrom,
                sandboxMode,
                dockerHost);
        dockerAdapter = new PlanktonSetupDockerAdapter(
                namespace,
                sandboxMode,
                dockerHost,
                paths,
                runningFrom);
        composeDocument = new PlanktonSetupComposeDocument(paths);
        setupPipeline = new PlanktonSetupPipeline(this, dockerAdapter, composeDocument);
    }

    public Pipeline getPipeline() {
        return setupPipeline.getPipeline();
    }
}
