package plankton.spring;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeDocumentConfiguration;
import plankton.compose.ComposeInitializer;
import plankton.docker.adapter.DockerAdapter;
import plankton.docker.adapter.DockerAdapterConfiguration;
import plankton.docker.client.DockerClient;
import plankton.docker.daemon.DockerDaemon;
import plankton.docker.daemon.DockerHostDaemon;
import plankton.docker.daemon.DockerSandboxConfiguration;
import plankton.docker.daemon.DockerSandboxDaemon;
import plankton.docker.inspect.Container;
import plankton.docker.inspect.ContainerMount;
import plankton.docker.inspect.DockerInspect;
import plankton.docker.util.DockerUtils;
import plankton.pipeline.ContainerRuntimeAdapter;
import plankton.pipeline.Pipeline;
import plankton.pipeline.PipelineConfiguration;
import plankton.pipeline.PipelineInitializer;
import plankton.util.BashScript;
import plankton.util.BashScriptFailedException;

@Component
public class PlanktonSetup {

    private final String workspace;
    private final String file;
    private final boolean sandboxEnabled;
    private final String dockerHostSocketAddress;
    private final List<String> target;
    private final List<String> skip;
    private final String namespace;
    private final DockerHostDaemon dockerHostDaemon;
    private final DockerClient dockerHostClient;
    private final String workspacePathFromPlanktonPerspective;
    private final String workspacePathOnHost;
    private final String workspacePathOnSandbox;
    private final String workspacePathFromAdapterPerspective;
    private final String composeFilePathFromPlanktonPerspective;
    private final String composeFilePathRelativeToWorkspace;
    private final String composeFilePathFromAdapterPerspective;
    private final ComposeDocument composeDocument;
    private final boolean runningFromHost;
    private final String runningFromContainerId;
    private final DockerDaemon dockerAdapterDaemon;
    private final DockerAdapter dockerAdapter;

    @Getter
    private final Pipeline pipeline;

    private final Logger logger = LoggerFactory.getLogger(PlanktonSetup.class);

    public PlanktonSetup(@Autowired PlanktonConfiguration planktonConfiguration) {
        workspace = workspace(planktonConfiguration.getWorkspace());
        file = file(planktonConfiguration.getFile());
        sandboxEnabled = planktonConfiguration.isSandbox();
        dockerHostSocketAddress = dockerHostSocketAddress(planktonConfiguration.getDocker());
        target = split(planktonConfiguration.getTarget());
        skip = split(planktonConfiguration.getSkip());
        namespace = namespace();
        dockerHostDaemon = new DockerHostDaemon(() -> dockerHostSocketAddress);
        dockerHostClient = new DockerClient(dockerHostDaemon);
        workspacePathFromPlanktonPerspective = workspacePathFromPlanktonPerspective();
        composeFilePathFromPlanktonPerspective = composeFilePathFromPlanktonPerspective();
        composeFilePathRelativeToWorkspace = composeFilePathRelativeToWorkspace();
        runningFromContainerId = DockerUtils.getCurrentContainerId();
        runningFromHost = (runningFromContainerId == null);
        if (runningFromHost) {
            workspacePathOnHost = workspacePathFromPlanktonPerspective;
        } else {
            workspacePathOnHost = workspacePathOnHostWhenRunningFromContainer();
        }
        if (sandboxEnabled) {
            workspacePathOnSandbox = "/sandbox-workspace";
            dockerAdapterDaemon = dockerSandboxDaemon();
            workspacePathFromAdapterPerspective = workspacePathOnSandbox;
        } else {
            workspacePathOnSandbox = null;
            dockerAdapterDaemon = dockerHostDaemon;
            workspacePathFromAdapterPerspective = workspacePathOnHost;
        }
        composeFilePathFromAdapterPerspective = workspacePathFromAdapterPerspective
                + composeFilePathRelativeToWorkspace;
        composeDocument = composeDocument();
        dockerAdapter = dockerAdapter();
        pipeline = pipeline();

        logger.info("workspace: {}", workspace);
        logger.info("file: {}", file);
        logger.info("sandboxEnabled: {}", sandboxEnabled);
        logger.info("dockerHostSocketAddress: {}", dockerHostSocketAddress);
        logger.info("target: {}", target);
        logger.info("namespace: {}", namespace);
        logger.info("dockerHostDaemon: {}", dockerHostDaemon);
        logger.info("dockerHostClient: {}", dockerHostClient);
        logger.info("workspacePathFromPlanktonPerspective: {}", workspacePathFromPlanktonPerspective);
        logger.info("workspacePathOnHost: {}", workspacePathOnHost);
        logger.info("workspacePathOnSandbox: {}", workspacePathOnSandbox);
        logger.info("workspacePathFromAdapterPerspective: {}", workspacePathFromAdapterPerspective);
        logger.info("composeFilePathFromPlanktonPerspective: {}", composeFilePathFromPlanktonPerspective);
        logger.info("composeFilePathRelativeToWorkspace: {}", composeFilePathRelativeToWorkspace);
        logger.info("composeFilePathFromAdapterPerspective: {}", composeFilePathFromAdapterPerspective);
        logger.info("composeDocument: {}", composeDocument);
        logger.info("runningFromHost: {}", runningFromHost);
        logger.info("runningFromContainerId: {}", runningFromContainerId);
        logger.info("dockerAdapterDaemon: {}", dockerAdapterDaemon);
        logger.info("dockerAdapter: {}", dockerAdapter);
    }

    private String workspace(String ws) {
        return (ws == null || ws.isBlank())
                ? "."
                : ws;
    }

    private String file(String f) {
        return (f == null || f.isBlank())
                ? "plankton-compose.yaml"
                : f;
    }

    private String dockerHostSocketAddress(String s) {
        return (dockerHostSocketAddress == null || dockerHostSocketAddress.isBlank())
                ? "unix:///var/run/docker.sock"
                : s;
    }

    private List<String> split(String s) {
        return (s == null || s.isBlank())
                ? new ArrayList<>()
                : Arrays.asList(s.split(","));
    }

    private String namespace() {
        int min = 1;
        int max = 1000000000;
        double r = (Math.random() * ((max - min) + 1)) + min;
        String a = String.valueOf((int) r);
        String b = String.valueOf(Instant.now().getEpochSecond()).trim();
        logger.debug("namespace: a={}; b={}", a, b);
        return a + b;
    }

    private String workspacePathFromPlanktonPerspective() {
        try {
            return BashScript.runAndGetOutputString("realpath " + workspace);
        } catch (BashScriptFailedException e) {
            throw new PlanktonSetupException("Unable to initialize the workspace path from Plankton perspective", e);
        }
        // TODO Check if directory exist
    }

    private String composeFilePathFromPlanktonPerspective() {
        String result;
        try {
            result = BashScript
                    .runAndGetOutputString("cd " + workspacePathFromPlanktonPerspective + " && realpath " + file);
        } catch (BashScriptFailedException e) {
            throw new PlanktonSetupException("Unable to initialize the compose file path from Plankton perspective", e);
        }
        if (!result.startsWith(workspacePathFromPlanktonPerspective))
            throw new PlanktonSetupException("Compose file must be inside workspace");
        return result;
        // TODO Check if file exists
    }

    private String composeFilePathRelativeToWorkspace() {
        return composeFilePathFromPlanktonPerspective.substring(workspacePathFromPlanktonPerspective.length());
    }

    private ComposeDocument composeDocument() {
        ComposeInitializer composeInitializer = new ComposeInitializer(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(composeFilePathFromPlanktonPerspective);
            }

            @Override
            public Path resolvePathsFrom() {
                return Paths.get(composeFilePathFromAdapterPerspective).getParent();
            }
        });
        return composeInitializer.composeDocument();
    }

    private String workspacePathOnHostWhenRunningFromContainer() {
        DockerInspect dockerInspect = new DockerInspect(dockerHostClient);
        Container container = dockerInspect.getContainer(runningFromContainerId);
        for (ContainerMount mount : container.getMounts()) {
            if (mount.getDestination().equals(workspacePathFromPlanktonPerspective)) {
                return mount.getSource();
            }
        }
        throw new PlanktonSetupException(
                "When running Plankton from within a container, you must bind the workspace directory");
        // TODO what if destination is a super directory of project path?
    }

    private DockerSandboxDaemon dockerSandboxDaemon() {
        return new DockerSandboxDaemon(new DockerSandboxConfiguration() {

            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public String dockerHostSocketAddress() {
                return dockerHostSocketAddress;
            }

            @Override
            public String underlyingWorkspaceDirectoryPath() {
                return workspacePathOnHost;
            }

            @Override
            public String workspaceDirectoryPath() {
                return workspacePathOnHost;
            }

            @Override
            public boolean runningFromHost() {
                return runningFromHost;
            }

            @Override
            public String runningFromContainerId() {
                return runningFromContainerId;
            }
        });
    }

    private DockerAdapter dockerAdapter() {
        return new DockerAdapter(new DockerAdapterConfiguration() {

            @Override
            public DockerDaemon dockerDaemon() {
                return dockerAdapterDaemon;
            }

            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public String workspacePathFromRunnerPerspective() {
                return workspacePathFromPlanktonPerspective;
            }

            @Override
            public String workspacePathFromAdapterPerspective() {
                return workspacePathFromAdapterPerspective;
            }
        });
    }

    private Pipeline pipeline() {
        PipelineConfiguration pipelineConfiguration = new PipelineConfiguration() {

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public ContainerRuntimeAdapter containerRuntimeAdapter() {
                return dockerAdapter;
            }

            @Override
            public Set<String> targetJobs() {
                return new HashSet<>(target);
            }

            @Override
            public Duration timeoutLimitForJobs() {
                return Duration.of(10L, ChronoUnit.MINUTES);
            }

            @Override
            public Set<String> skipJobs() {
                return new HashSet<>(skip);
            }
        };
        PipelineInitializer pipelineInitializer = new PipelineInitializer(pipelineConfiguration);
        return pipelineInitializer.pipeline();
    }
}
