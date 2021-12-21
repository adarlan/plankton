package plankton.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.EqualsAndHashCode;
import plankton.compose.serviceprops.Build;
import plankton.compose.serviceprops.Command;
import plankton.compose.serviceprops.DependsOn;
import plankton.compose.serviceprops.Entrypoint;
import plankton.compose.serviceprops.EnvFile;
import plankton.compose.serviceprops.Environment;
import plankton.compose.serviceprops.Expose;
import plankton.compose.serviceprops.Extends;
import plankton.compose.serviceprops.GroupAdd;
import plankton.compose.serviceprops.Healthcheck;
import plankton.compose.serviceprops.Image;
import plankton.compose.serviceprops.Labels;
import plankton.compose.serviceprops.Profiles;
import plankton.compose.serviceprops.User;
import plankton.compose.serviceprops.Volumes;
import plankton.compose.serviceprops.WorkingDir;

@EqualsAndHashCode(of = { "key" })
public class ComposeService {

    static final String PARENT_KEY = "services";

    ComposeDocument composeDocument;
    String name;
    String key;
    final List<ServiceProperty> properties = new ArrayList<>();

    Build build;
    Command command;
    DependsOn dependsOn;
    Entrypoint entrypoint;
    EnvFile envFile;
    Environment environment;
    Expose expose;
    Extends extends1;
    GroupAdd groupAdd;
    Healthcheck healthcheck;
    Image image;
    Labels labels;
    Profiles profiles;
    User user;
    Volumes volumes;
    WorkingDir workingDir;

    final List<ComposeService> parentServices = new ArrayList<>();
    final Set<ComposeService> childServices = new HashSet<>();

    final Set<ComposeService> dependencies = new HashSet<>();
    final Set<ComposeService> dependents = new HashSet<>();

    ComposeService() {
        super();
    }

    public ComposeDocument composeDocument() {
        return composeDocument;
    }

    public String name() {
        return name;
    }

    public List<ComposeService> parentServices() {
        return Collections.unmodifiableList(parentServices);
    }

    public Set<ComposeService> childServices() {
        return Collections.unmodifiableSet(childServices);
    }

    @Override
    public String toString() {
        return name;
    }

    public Optional<Build> build() {
        return Optional.ofNullable(build);
    }

    public List<String> command() {
        return command == null
                ? new ArrayList<>()
                : command.lines();
    }

    public Map<ComposeService, DependsOnCondition> dependsOn() {
        Map<ComposeService, DependsOnCondition> map = new HashMap<>();
        if (dependsOn != null) {
            dependsOn.serviceConditionMap().forEach((serviceName, condition) -> {
                ComposeService service = composeDocument.getServiceByName(serviceName);
                map.put(service, condition);
            });
        }
        return map;
    }

    public List<String> entrypoint() {
        return entrypoint == null
                ? new ArrayList<>()
                : entrypoint.lines();
    }

    public boolean entrypointIsReseted() {
        return entrypoint != null && entrypoint.isReseted();
    }

    public List<String> envFile() {
        return envFile == null
                ? new ArrayList<>()
                : envFile.list();
    }

    public List<String> environment() {
        return environment == null
                ? new ArrayList<>()
                : environment.list();
    }

    public List<String> expose() {
        return expose == null
                ? new ArrayList<>()
                : expose.ports();
    }

    public Optional<Extends> extends1() {
        return Optional.ofNullable(extends1);
    }

    public List<String> groupAdd() {
        return groupAdd == null
                ? new ArrayList<>()
                : groupAdd.list();
    }

    public Optional<Healthcheck> healthcheck() {
        return Optional.ofNullable(healthcheck);
    }

    public Optional<String> image() {
        return Optional.ofNullable(image).map(Image::getTag);
    }

    public List<String> labels() {
        return labels == null
                ? new ArrayList<>()
                : labels.list();
    }

    public List<String> profiles() {
        return profiles == null
                ? new ArrayList<>()
                : profiles.list();
    }

    public Optional<User> user() {
        return Optional.ofNullable(user);
    }

    public List<String> volumes() {
        return volumes == null
                ? new ArrayList<>()
                : volumes.list();
    }

    public Optional<String> workingDir() {
        return Optional.ofNullable(workingDir).map(WorkingDir::getPath);
    }
}
