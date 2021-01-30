package me.adarlan.plankton.compose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import lombok.experimental.Accessors;
import me.adarlan.plankton.util.Colors;

@EqualsAndHashCode(of = { "composeDocument", "name" })
@Accessors(fluent = true)
public class ComposeService {
    public static final String PARENT_KEY = "services";

    @Getter
    private final ComposeDocument composeDocument;

    @Getter
    private final String name;

    private final Map<String, Object> propertiesMap;
    private final Set<String> ignoredKeys = new HashSet<>();
    boolean valid = true;

    private final Build build;
    private final Command command;
    private final DependsOn dependsOn;
    private final Entrypoint entrypoint;
    private final Environment environment;
    private final EnvFile envFile;
    private final Expose expose;
    private final GroupAdd groupAdd;
    private final Healthcheck healthcheck;
    private final Image image;
    private final Labels labels;
    private final Profiles profiles;
    private final Scale scale;
    private final User user;
    private final Volumes volumes;
    private final VolumesFrom volumesFrom;
    private final WorkingDir workingDir;

    final Extends extends1;

    private static final Logger logger = LoggerFactory.getLogger(ComposeService.class);
    private final String colorizedName;

    ComposeService(ComposeDocument composeDocument1, String name1, Map<String, Object> propertiesMap1) {
        this.composeDocument = composeDocument1;
        this.name = name1;
        this.propertiesMap = propertiesMap1;
        this.colorizedName = Colors.colorized(name);

        this.build = initialize(Build.KEY, Build::new);
        this.command = initialize(Command.KEY, Command::new);
        this.dependsOn = initialize(DependsOn.KEY, DependsOn::new);
        this.entrypoint = initialize(Entrypoint.KEY, Entrypoint::new);
        this.environment = initialize(Environment.KEY, Environment::new);
        this.envFile = initialize(EnvFile.KEY, EnvFile::new);
        this.expose = initialize(Expose.KEY, Expose::new);
        this.extends1 = initialize(Extends.KEY, Extends::new);
        this.groupAdd = initialize(GroupAdd.KEY, GroupAdd::new);
        this.healthcheck = initialize(Healthcheck.KEY, Healthcheck::new);
        this.image = initialize(Image.KEY, Image::new);
        this.labels = initialize(Labels.KEY, Labels::new);
        this.profiles = initialize(Profiles.KEY, Profiles::new);
        this.scale = initialize(Scale.KEY, Scale::new);
        this.user = initialize(User.KEY, User::new);
        this.volumes = initialize(Volumes.KEY, Volumes::new);
        this.volumesFrom = initialize(VolumesFrom.KEY, VolumesFrom::new);
        this.workingDir = initialize(WorkingDir.KEY, WorkingDir::new);

        warnIgnoredKeys();
    }

    private <T> T initialize(String key, Function<Object, T> function) {
        if (propertiesMap.containsKey(key)) {
            logger.info("Loading property: {}.{}.{}", PARENT_KEY, this, key);
            Object object = propertiesMap.remove(key);
            try {
                return function.apply(object);
            } catch (ClassCastException | ComposeFileFormatException e) {
                valid = false;
                logger.error("Loading property: {}.{}.{} -> {}", PARENT_KEY, this, key, e.getMessage(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    private void ignoredKeys(String parentKey, Set<String> keys) {
        keys.forEach(key -> ignoredKeys.add(parentKey + "." + key));
    }

    private void warnIgnoredKeys() {
        ignoredKeys.addAll(propertiesMap.keySet());
        ignoredKeys.forEach(key -> logger.warn("Ignoring key: {}.{}.{}", PARENT_KEY, this, key));
    }

    void extendsFrom(ComposeService other) {
        // TODO extend service
        // it can call consecutive compose file initializations
        // warn overriding keys
    }

    public Optional<Build> build() {
        return (Optional.ofNullable(build));
    }

    public List<String> command() {
        return (command == null ? new ArrayList<>() : command.list);
    }

    public Map<String, DependsOnCondition> dependsOn() {
        return (Collections.unmodifiableMap(dependsOn == null ? new HashMap<>() : dependsOn.serviceConditionMap));
    }

    public List<String> entrypoint() {
        return (entrypoint == null ? new ArrayList<>() : entrypoint.list);
    }

    public List<String> environment() {
        return (Collections.unmodifiableList(environment == null ? new ArrayList<>() : environment.list));
    }

    public List<String> envFile() {
        return (Collections.unmodifiableList(envFile == null ? new ArrayList<>() : envFile.list));
    }

    public List<Integer> expose() {
        return (Collections.unmodifiableList(expose == null ? new ArrayList<>() : expose.ports));
    }

    public List<String> groupAdd() {
        return (Collections.unmodifiableList(groupAdd == null ? new ArrayList<>() : groupAdd.groups));
    }

    public Optional<Healthcheck> healthcheck() {
        return (Optional.ofNullable(healthcheck));
    }

    public Optional<String> imageTag() { // TODO rename -> image()
        return Optional.ofNullable(image).map(i -> i.tag);
    }

    public List<String> labels() {
        return (Collections.unmodifiableList(labels == null ? new ArrayList<>() : labels.list));
    }

    public List<String> profiles() {
        return (Collections.unmodifiableList(profiles == null ? new ArrayList<>() : profiles.list));
    }

    public Integer scale() {
        return (scale == null ? 1 : scale.number);
    }

    public Optional<String> user() {
        return (Optional.ofNullable(user).map(u -> u.name));
    }

    public List<String> volumes() {
        return (Collections.unmodifiableList(volumes == null ? new ArrayList<>() : volumes.list));
    }

    public List<String> volumesFrom() {
        return (Collections.unmodifiableList(volumesFrom == null ? new ArrayList<>() : volumesFrom.list));
    }

    public Optional<String> workingDir() {
        return (Optional.ofNullable(workingDir).map(w -> w.path));
    }

    @Override
    public String toString() {
        return colorizedName;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMapOfObjects(Object object) {
        return (Map<String, Object>) object;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castToMapOfStrings(Object object) {
        return (Map<String, String>) object;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> castToMapOfMaps(Object object) {
        return (Map<String, Map<String, Object>>) object;
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object object) {
        return (List<String>) object;
    }

    private List<String> convertToKeyValueList(Map<String, String> keyValueMap) {
        List<String> list = new ArrayList<>();
        keyValueMap.forEach((k, v) -> list.add(k + "=" + v));
        return list;
    }

    private String validateAndExpandPath(String path) {
        return path;

        // TODO check if the path is relative (don't start with `/`)

        // TODO expand to the full path from inside the direcory which contains the
        // compose file
        // cd $composeDirectoryPath
        // get-full-path-of $path
    }

    public class Build {
        public static final String KEY = "build";
        public final String context;
        public final String dockerfile;

        private Build(Object object) {
            if (object instanceof String) {
                String c = (String) object;
                context = validateAndExpandPath(c);
                dockerfile = null;
            } else {
                Map<String, Object> map = castToMapOfObjects(object);
                String c = (String) map.remove("context");
                context = validateAndExpandPath(c);
                String d = (String) map.remove("dockerfile");
                dockerfile = validateAndExpandPath(d);
                ignoredKeys(KEY, map.keySet());
            }
        }
    }

    public class Command {
        public static final String KEY = "command";
        public final List<String> list;

        private Command(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else
                list = castToStringList(object);
        }
    }

    public class DependsOn {
        public static final String KEY = "depends_on";
        public final Map<String, DependsOnCondition> serviceConditionMap = new HashMap<>();

        private DependsOn(Object object) {
            if (object instanceof String) {
                serviceConditionMap.put((String) object, DependsOnCondition.EXIT_ZERO);
            } else if (object instanceof List) {
                List<String> list = castToStringList(object);
                list.forEach(serviceName -> serviceConditionMap.put(serviceName, DependsOnCondition.EXIT_ZERO));
            } else if (object instanceof Map) {
                Map<String, Map<String, Object>> m = castToMapOfMaps(object);
                m.forEach((serviceName, serviceMap) -> {
                    String conditionString = (String) serviceMap.remove("condition");
                    DependsOnCondition condition = DependsOnCondition.of(conditionString);
                    serviceConditionMap.put(serviceName, condition);
                    ignoredKeys(KEY + "." + serviceName, serviceMap.keySet());
                });
            }
        }
    }

    public class Entrypoint {
        public static final String KEY = "entrypoint";
        public final List<String> list;

        private Entrypoint(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else
                list = castToStringList(object);
        }
    }

    public class Environment {
        public static final String KEY = "environment";
        public final List<String> list;

        private Environment(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else if (object instanceof Map) {
                Map<String, String> keyValueMap = castToMapOfStrings(object);
                list = convertToKeyValueList(keyValueMap);
            } else
                list = castToStringList(object);
        }
    }

    public class EnvFile {
        public static final String KEY = "env_file";
        public final List<String> list;

        private EnvFile(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else {
                list = new ArrayList<>();
                List<String> l = castToStringList(object);
                l.forEach(filePath -> list.add(validateAndExpandPath(filePath)));
            }
        }
    }

    public class Expose {
        public static final String KEY = "expose";
        public final List<Integer> ports;

        private Expose(Object object) {
            ports = new ArrayList<>();
        }
    }

    public class Extends {
        public static final String KEY = "extends";
        public final String file;
        public final String service;

        private Extends(Object object) {
            if (object instanceof String) {
                file = null;
                service = (String) object;
            } else {
                Map<String, Object> map = castToMapOfObjects(object);
                file = validateAndExpandPath((String) map.remove("file"));
                service = (String) map.remove("service");
                ignoredKeys(KEY, map.keySet());
            }
        }
    }

    public class GroupAdd {
        public static final String KEY = "group_add";
        public final List<String> groups;

        private GroupAdd(Object object) {
            groups = castToStringList(object);
        }
    }

    public class Healthcheck {
        public static final String KEY = "healthcheck";
        public final boolean disable;
        public final String test;
        public final String interval;
        public final String timeout;
        public final String retries;
        public final String startPeriod;

        private Healthcheck(Object object) {
            Map<String, Object> map = castToMapOfObjects(object);
            if (map.containsKey("disable"))
                disable = (boolean) map.remove("disable");
            else
                disable = false;
            test = (String) map.remove("test");
            interval = (String) map.remove("interval");
            timeout = (String) map.remove("timeout");
            retries = (String) map.remove("retries");
            startPeriod = (String) map.remove("start_period");
            ignoredKeys(KEY, map.keySet());
        }
    }

    public class Image {
        public static final String KEY = "image";
        public final String tag;

        private Image(Object object) {
            tag = (String) object;
        }
    }

    public class Labels {
        public static final String KEY = "labels";
        public final List<String> list;

        private Labels(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else if (object instanceof Map) {
                Map<String, String> keyValueMap = castToMapOfStrings(object);
                list = convertToKeyValueList(keyValueMap);
            } else
                list = castToStringList(object);
        }
    }

    public class Profiles {
        public static final String KEY = "profiles";
        public final List<String> list;

        private Profiles(Object object) {
            list = castToStringList(object);
        }
    }

    public class Scale {
        public static final String KEY = "scale";
        public final Integer number;

        private Scale(Object object) {
            number = ((Number) object).intValue();
        }
    }

    public class User {
        public static final String KEY = "user";
        public final String name;

        private User(Object object) {
            name = (String) object;
        }
    }

    public class Volumes {
        public static final String KEY = "volumes";
        public final List<String> list;

        private Volumes(Object object) {
            list = castToStringList(object);
            // TODO add support for super-short-syntax (a string)
            // and for long-syntax (a list of maps)
        }
    }

    public class VolumesFrom {
        public static final String KEY = "volumes_from";
        public final List<String> list;

        private VolumesFrom(Object object) {
            list = castToStringList(object);
            // TODO replace each service name by its first container name
        }
    }

    public class WorkingDir {
        public static final String KEY = "working_dir";
        public final String path;

        private WorkingDir(Object object) {
            path = (String) object;
        }
    }
}
