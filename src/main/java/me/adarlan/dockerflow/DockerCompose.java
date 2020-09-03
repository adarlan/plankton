package me.adarlan.dockerflow;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import lombok.Getter;
import me.adarlan.dockerflow.config.DockerflowConfig;
import me.adarlan.dockerflow.bash.BashScript;

@Component
public class DockerCompose {

    // TODO validar version = 3.x | x >= (?)
    // TODO container_name deve ser ignorado

    private final String originalFile;
    private final String file;
    final String projectName;
    private final String projectDirectory;

    private final List<String> environmentVariables;

    private static final String BASE_COMMAND = "docker-compose";
    String options;

    private Map<String, Object> document;

    @Getter
    private Map<String, Object> services;

    @Autowired
    public DockerCompose(DockerflowConfig dockerflowConfig, List<String> environmentVariables) {
        originalFile = dockerflowConfig.getFile();
        file = ".dockerflow.docker-compose.yml";
        projectName = dockerflowConfig.getName();
        projectDirectory = dockerflowConfig.getWorkspace();
        this.environmentVariables = environmentVariables;

        this.initializeOptions();
        this.initializeFile();
        this.validateFile();
        this.prune();
        this.createNetwork();
        this.initializeDocument();
        // this.initializeMaxServiceName();
    }

    private void initializeOptions() {
        // TODO --host quando tiver usando dind
        options = "--no-ansi --project-name " + projectName + " --file " + file + " --project-directory "
                + projectDirectory;
    }

    private void initializeFile() {
        // TODO garantir que o caractere § não é usado no arquivo
        // e se tiver 3 ou mais $ em sequência?
        String scriptName = "initialize-docker-compose-file";
        BashScript script = new BashScript(scriptName);
        script.env(environmentVariables);
        String expandedFileTemp = file + ".temp";
        script.command("cp " + originalFile + " " + expandedFileTemp);
        script.command("sed --in-place 's/\\$\\$/§/g' " + expandedFileTemp);
        script.command("cat " + expandedFileTemp + " | envsubst > " + file);
        script.command("sed --in-place 's/§/\\$\\$/g' " + file);
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        try {
            script.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (script.getExitCode() != 0) {
            throw new DockerflowException("Unable to initialize the Docker Compose file");
        }
    }

    private void validateFile() {
        // String configOptions = "";
        // if (!local) {
        // configOptions = "--resolve-image-digests";
        // // TODO verificar se isso funcona com 'build' ao invés de 'image'... NÃO
        // FUNCIONA
        // }
        String scriptName = "validate-docker-compose-file";
        BashScript script = new BashScript(scriptName);
        // script.command("docker-compose " + options + " config " + configOptions);
        script.command(BASE_COMMAND + " " + options + " config");
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        try {
            script.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (script.getExitCode() != 0) {
            throw new DockerflowException("The Docker Compose file is invalid");
        }
    }

    private void prune() {
        String scriptName = "prune-docker";
        BashScript script = new BashScript(scriptName);
        // script.command("docker container prune -f");
        // script.command("docker network prune -f");
        script.command(BASE_COMMAND + " " + options + " down --volumes --remove-orphans");
        // script.command("docker container prune -f");
        // script.command("docker network prune -f");
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        try {
            script.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (script.getExitCode() != 0) {
            throw new DockerflowException("Unable to clean the Docker Compose elements");
        }
    }

    private void createNetwork() {
        String scriptName = "create-network";
        BashScript script = new BashScript(scriptName);
        String networkName = projectName + "_default";
        script.command("docker network create --attachable " + networkName);
        script.forEachOutput(line -> Logger.debug(() -> scriptName + " >> " + line));
        script.forEachError(line -> Logger.error(() -> scriptName + " >> " + line));
        try {
            script.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (script.getExitCode() != 0) {
            throw new DockerflowException("Unable to create the Docker network: " + networkName);
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeDocument() {
        try (FileInputStream fileInputStream = new FileInputStream(file);) {
            final Yaml yaml = new Yaml();
            document = yaml.load(fileInputStream);
        } catch (IOException e) {
            throw new DockerflowException("Unable to initialize the Docker Compose document", e);
        }
        services = (Map<String, Object>) document.get("services");
    }

    public boolean createContainers(Job job) throws InterruptedException {
        final List<String> logs = job.logs;
        final String scriptName = "create-containers_" + job.name;
        final BashScript script = new BashScript(scriptName);
        final String upOptions = "--build --no-start --scale " + job.name + "=" + job.scale;
        script.command(BASE_COMMAND + " " + options + " up " + upOptions + " " + job.name);
        script.forEachOutputAndError(line -> {
            synchronized (logs) {
                logs.add(line);
                Logger.debug(() -> scriptName + " >> " + line);
            }
        });
        script.run();
        return script.getExitCode() == 0;
    }

    /*
     * private void initializeMaxServiceName() { for (String name :
     * services.keySet()) if (name.length() > Logger.maxJobName)
     * Logger.setMaxJobName(name.length()); }
     */

    /*
     * public List<String> containerNamesOf(Job job) { List<String> containerNames =
     * new ArrayList<>(); for (int i = 1; i <= job.scale; i++) { String
     * containerName = projectName + "_" + job.name + "_" + i;
     * containerNames.add(containerName); } return containerNames; }
     */

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
        throw new DockerflowException(
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
}