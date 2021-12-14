package plankton;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import plankton.bash.BashScript;
import plankton.bash.BashScriptFailedException;
import plankton.compose.ComposeDocument;
import plankton.compose.ComposeDocumentConfiguration;
import plankton.core.ContainerRuntimeAdapter;
import plankton.core.Pipeline;
import plankton.core.PipelineConfiguration;
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

@Component
public class PlanktonSetup {

    private final String workspace;
    private final String file;
    private final boolean sandboxEnabled;
    private final String dockerHostSocketAddress;
    private final List<String> target;
    private final String namespace;
    private final DockerHostDaemon dockerHostDaemon;
    private final DockerClient dockerHostClient;
    private final String workspacePathFromPlanktonPerspective;
    private final String workspacePathOnHost;
    private final String workspacePathOnSandbox;
    private final String workspacePathFromAdapterPerspective;
    private final String composeFilePathFromPlanktonPerspective;
    private final ComposeDocument composeDocument;
    private final boolean runningFromHost;
    private final String runningFromContainerId;
    private final DockerDaemon dockerAdapterDaemon;
    private final DockerAdapter dockerAdapter;

    @Getter
    private final Pipeline pipeline;

    public PlanktonSetup(@Autowired PlanktonConfiguration planktonConfiguration) {
        workspace = workspace(planktonConfiguration.getWorkspace());
        file = file(planktonConfiguration.getFile());
        sandboxEnabled = planktonConfiguration.isSandbox();
        dockerHostSocketAddress = dockerHostSocketAddress(planktonConfiguration.getDocker());
        target = target(planktonConfiguration.getTarget());
        namespace = namespace();
        dockerHostDaemon = new DockerHostDaemon(() -> dockerHostSocketAddress);
        dockerHostClient = new DockerClient(dockerHostDaemon);
        workspacePathFromPlanktonPerspective = workspacePathFromPlanktonPerspective();
        composeFilePathFromPlanktonPerspective = composeFilePathFromPlanktonPerspective();
        composeDocument = composeDocument();
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
        dockerAdapter = dockerAdapter();
        pipeline = pipeline();
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
                ? "/var/run/docker.sock"
                : s;
    }

    private List<String> target(String s) {
        return (s == null || s.isBlank())
                ? new ArrayList<>()
                : Arrays.asList(s.split(","));
    }

    private String namespace() {
        return String.valueOf(Instant.now().getEpochSecond()).trim();
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

    private ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public Path filePath() {
                return Paths.get(composeFilePathFromPlanktonPerspective);
            }

            @Override
            public Path resolvePathsFrom() {
                return Paths.get(composeFilePathFromPlanktonPerspective).getParent();
            }

            @Override
            public Set<String> targetServices() {
                return new HashSet<>(target);
            }
        });
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
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

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
        return new Pipeline(new PipelineConfiguration() {

            @Override
            public ComposeDocument composeDocument() {
                return composeDocument;
            }

            @Override
            public ContainerRuntimeAdapter containerRuntimeAdapter() {
                return dockerAdapter;
            }
        });
    }
}
