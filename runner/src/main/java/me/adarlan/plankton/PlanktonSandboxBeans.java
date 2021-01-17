package me.adarlan.plankton;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.adarlan.plankton.compose.Compose;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.compose.ComposeDocumentConfiguration;
import me.adarlan.plankton.docker.DockerCompose;
import me.adarlan.plankton.sandbox.Sandbox;
import me.adarlan.plankton.sandbox.SandboxConfiguration;
import me.adarlan.plankton.workflow.Pipeline;

@Configuration
@ConditionalOnExpression("'${plankton.runner.mode}'=='sandbox'")
public class PlanktonSandboxBeans {

    @Value("${plankton.compose.file}")
    private String composeFile;

    @Value("${plankton.workspace}")
    private String workspace;

    @Value("${plankton.metadata}")
    private String metadata;

    @Value("${plankton.docker.host}")
    private String dockerHost;

    @Bean
    public Sandbox sandbox() {
        return new Sandbox(new SandboxConfiguration() {

            @Override
            public boolean fromHost() {
                return true;
            }

            @Override
            public String id() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String workspaceDirectoryOnHost() {
                return workspace;
            }
        });
    }

    @Bean
    public ComposeDocument composeDocument() {
        return new ComposeDocument(new ComposeDocumentConfiguration() {

            @Override
            public String projectName() {
                return String.valueOf(Instant.now().getEpochSecond());
            }

            @Override
            public String filePath() {
                return expandUserHomeTilde(composeFile);
            }

            @Override
            public String projectDirectory() {
                return expandUserHomeTilde(workspace);
            }

            @Override
            public String metadataDirectory() {
                return expandUserHomeTilde(metadata);
            }

            private String expandUserHomeTilde(String path) {
                if (path.startsWith("~"))
                    return path.replaceFirst("^~", System.getProperty("user.home"));
                else
                    return path;
            }
        });
    }

    @Bean
    public Compose compose() {
        return new DockerCompose(() -> sandbox().getSocketAddress(), composeDocument());
    }

    @Bean
    public Pipeline pipeline() {
        return new Pipeline(compose());
    }
}
