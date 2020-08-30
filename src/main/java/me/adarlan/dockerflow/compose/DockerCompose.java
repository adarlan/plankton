package me.adarlan.dockerflow.compose;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import me.adarlan.dockerflow.DockerflowConfig;
import me.adarlan.dockerflow.bash.BashScript;
import me.adarlan.dockerflow.bash.BashScriptRunner;

@Component
public class DockerCompose {

    // TODO validar version = 3.x

    private final String metadata;
    private final String logs;
    private final boolean local;

    private final String originalFile;
    private final String file;
    private final String projectName;
    private final String projectDirectory;

    private final String envFile;
    private final List<String> environmentVariables = new ArrayList<>();

    private String command;

    private Map<String, Object> document;

    @Getter
    private Map<String, Object> services;

    private final BashScriptRunner bashScriptRunner;

    @Autowired
    public DockerCompose(DockerflowConfig dockerflowConfig, BashScriptRunner bashScriptRunner) {
        metadata = dockerflowConfig.getMetadata();
        logs = metadata + "/logs";
        local = dockerflowConfig.isLocal();
        originalFile = dockerflowConfig.getFile();
        file = metadata + "/docker-compose.yml";
        projectName = dockerflowConfig.getName();
        projectDirectory = dockerflowConfig.getWorkspace();
        envFile = dockerflowConfig.getEnvironment();

        this.bashScriptRunner = bashScriptRunner;

        this.initializeCommand();
        this.initializeEnvironmentVariables();
        this.initializeFile(); // TODO => initializeMetadata?
        this.validateFile();
        if (dockerflowConfig.isLocal()) {
            this.prune();
        }
        this.createNetwork();
        this.initializeDocument();
    }

    private void initializeCommand() {
        // TODO --host
        command = "docker-compose";
        command += " --no-ansi";
        command += " --project-name " + projectName;
        command += " --file " + file;
        command += " --project-directory " + projectDirectory;
    }

    private void initializeEnvironmentVariables() {
        try (FileInputStream fileInputStream = new FileInputStream(envFile);
                Scanner scanner = new Scanner(fileInputStream);) {
            scanner.forEachRemaining(environmentVariables::add);
        } catch (FileNotFoundException e) {
            // TODO ignore?
        } catch (IOException e) {
            throw new DockerComposeException(e);
        }
    }

    private void initializeFile() {
        BashScript script = new BashScript();
        String expandedFileTemp = file + ".temp";
        if (local) {
            script.add("rm -rf " + metadata);
        }
        script.add("mkdir -p " + metadata);
        script.add("mkdir -p " + logs);
        script.add("cp " + originalFile + " " + expandedFileTemp);

        // TODO garantir que o caractere § não é usado no arquivo
        // e se tiver 3 ou mais $ em sequência?
        script.add("sed --in-place 's/\\$\\$/§/g' " + expandedFileTemp);
        script.add("cat " + expandedFileTemp + " | envsubst > " + file);
        script.add("sed --in-place 's/§/\\$\\$/g' " + file);

        script.env(environmentVariables);
        if (!bashScriptRunner.runSuccessfully(script)) {
            throw new DockerComposeException("Unable to initialize the Docker Compose file");
        }
    }

    private void validateFile() {
        String configOptions = "";
        if (!local) {
            configOptions = "--resolve-image-digests";
            // TODO verificar se isso funcona com 'build' ao invés de 'image'
        }
        if (!bashScriptRunner.runSuccessfully(new BashScript(command + " config " + configOptions))) {
            throw new DockerComposeException("The Docker Compose file is invalid");
        }
    }

    private void prune() {
        BashScript script = new BashScript();
        script.add("docker container prune -f");
        script.add("docker network prune -f");
        script.add(command + " down --volumes --remove-orphans");
        script.add("docker container prune -f");
        script.add("docker network prune -f");
        if (!bashScriptRunner.runSuccessfully(script)) {
            throw new DockerComposeException("Unable to clean the Docker Compose elements");
        }
    }

    private void createNetwork() {
        String networkName = projectName + "_default";
        if (!bashScriptRunner.runSuccessfully(new BashScript("docker network create --attachable " + networkName))) {
            throw new DockerComposeException("Unable to create the Docker Compose network");
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeDocument() {
        try (FileInputStream fileInputStream = new FileInputStream(file);) {
            final Yaml yaml = new Yaml();
            document = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new DockerComposeException("Unable to initialize the Docker Compose document", e);
        }
        services = (Map<String, Object>) document.get("services");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getService(String serviceName) {
        return (Map<String, Object>) services.get(serviceName);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getServicePropertyAsKeyValueMap(String serviceName, String propertyName) {
        Map<String, Object> service = getService(serviceName);
        Object property = service.get(propertyName);
        if (property == null) {
            return new HashMap<>();
        }
        if (property instanceof Map) {
            Map<String, Object> objMap = (Map<String, Object>) property;
            Map<String, String> strMap = new HashMap<>();
            objMap.forEach((k, v) -> {
                String strValue;
                if (v != null) {
                    strValue = v.toString();
                    strMap.put(k, strValue);
                }
            });
            return strMap;
        }
        if (property instanceof String) {
            List<String> list = new ArrayList<>();
            list.add((String) property);
            property = list;
        }
        if (property instanceof Collection) {
            Map<String, String> strMap = new HashMap<>();
            ((Collection<String>) property).forEach(labelString -> {
                int separatorIndex = labelString.indexOf("=");
                String key = labelString.substring(0, separatorIndex).trim();
                String value = labelString.substring(separatorIndex + 1).trim();
                strMap.put(key, value);
            });
            return strMap;
        }
        throw new DockerComposeException(
                "Unable to read the Docker Compose property: services." + serviceName + "." + propertyName);
    }

    @SuppressWarnings("unchecked")
    public List<String> getServicePropertyAsStringList(String serviceName, String propertyName) {
        Map<String, Object> dcService = getService(serviceName);
        Object property = dcService.get(propertyName);
        if (property == null) {
            return new ArrayList<>();
        }
        if (property instanceof Collection) {
            return ((Collection<Object>) property).stream().map(Object::toString).collect(Collectors.toList());
        }
        if (property instanceof String) {
            List<String> list = new ArrayList<>();
            list.add((String) property);
            return list;
        } else {
            List<String> list = new ArrayList<>();
            list.add(property.toString());
            return list;
        }
    }

    public BashScript getServiceUpBashScript(String serviceName, int scale) {
        String upCommand = command + " up";
        upCommand += " --build --force-recreate --abort-on-container-exit";
        upCommand += " --no-color --no-deps --renew-anon-volumes --remove-orphans";
        upCommand += " --exit-code-from " + serviceName;
        if (scale > 1) {
            upCommand += " --scale " + serviceName + "=" + scale;
        }
        upCommand += " " + serviceName;
        // TODO precisa add variáveis? as variáveis já foram expandidas no arquivo
        return new BashScript(upCommand);
        // .outputFilePath(logs + "/" + serviceName + ".log")
        // .errorFilePath(logs + "/" + serviceName + ".error.log");
    }

    private BashScript getServiceDownBashScript(String serviceName) {
        String downCommand = command + " stop";
        // downCommand += " --volumes --remove-orphans";
        downCommand += " " + serviceName;
        // TODO precisa add variáveis? as variáveis já foram expandidas no arquivo
        return new BashScript(downCommand);
    }

    public void stop(String serviceName) {
        if (!bashScriptRunner.runSuccessfully(getServiceDownBashScript(serviceName))) {
            throw new DockerComposeException("Unable to stop the container " + serviceName);
        }
    }
}