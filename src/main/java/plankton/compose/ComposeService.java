package plankton.compose;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import plankton.util.Colors;

@EqualsAndHashCode(of = { "composeDocument", "name" })
@Accessors(fluent = true)
public class ComposeService {
    public static final String PARENT_KEY = "services";

    @Getter
    private final ComposeDocument composeDocument;

    @Getter
    private final String name;

    private final Map<String, Object> propertiesMap;
    boolean valid = true;

    private String currentKey;
    private final Set<String> ignoredKeys = new HashSet<>();

    final Extends extends1;
    private final Set<String> overriddenKeys = new HashSet<>();

    private Build build;
    private Command command;
    private DependsOn dependsOn;
    private Entrypoint entrypoint;
    private Environment environment;
    private EnvFile envFile;
    private Expose expose;
    private GroupAdd groupAdd;
    private Healthcheck healthcheck;
    private Image image;
    private Labels labels;
    private Profiles profiles;
    private Scale scale;
    private User user;
    private Volumes volumes;
    private WorkingDir workingDir;

    @Getter
    private boolean entrypointIsReseted;

    private static final Logger logger = LoggerFactory.getLogger(ComposeService.class);
    private final String colorizedName;

    ComposeService(ComposeDocument composeDocument1, String name1, Map<String, Object> propertiesMap1) {
        this.composeDocument = composeDocument1;
        this.name = name1;
        this.propertiesMap = propertiesMap1;

        this.colorizedName = Colors.colorized(name);
        logger.debug("{} ... Properties map: {}", this, propertiesMap);

        this.extends1 = initialize(Extends.KEY, Extends::new);

        this.build = initialize(Build.KEY, Build::new);
        this.command = initialize(Command.KEY, Command::new);
        this.dependsOn = initialize(DependsOn.KEY, DependsOn::new);
        this.entrypoint = initialize(Entrypoint.KEY, Entrypoint::new);
        this.environment = initialize(Environment.KEY, Environment::new);
        this.envFile = initialize(EnvFile.KEY, EnvFile::new);
        this.expose = initialize(Expose.KEY, Expose::new);
        this.groupAdd = initialize(GroupAdd.KEY, GroupAdd::new);
        this.healthcheck = initialize(Healthcheck.KEY, Healthcheck::new);
        this.image = initialize(Image.KEY, Image::new);
        this.labels = initialize(Labels.KEY, Labels::new);
        this.profiles = initialize(Profiles.KEY, Profiles::new);
        this.scale = initialize(Scale.KEY, Scale::new);
        this.user = initialize(User.KEY, User::new);
        this.volumes = initialize(Volumes.KEY, Volumes::new);
        this.workingDir = initialize(WorkingDir.KEY, WorkingDir::new);

        warnIgnoredAndOverriddenKeys();
    }

    private <T> T initialize(String key, Function<Object, T> function) {
        if (propertiesMap.containsKey(key)) {
            currentKey = key;
            logger.debug("{}.{} ... Loading", this, key);
            Object object = propertiesMap.remove(key);
            try {
                return function.apply(object);
            } catch (ClassCastException | ComposeFileFormatException e) {
                valid = false;
                logger.error("{}.{} ... Error: {}", this, key, e.getMessage(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    private void ignoredKeys(String parentKey, Set<String> keys) {
        keys.forEach(key -> ignoredKeys.add(parentKey + "." + key));
    }

    // private void overriddenKeys(String parentKey, Set<String> keys) {
    // keys.forEach(key -> overriddenKeys.add(parentKey + "." + key));
    // }

    // private void overriddenKey(String key) {
    // overriddenKeys.add(key);
    // }

    private void warnIgnoredAndOverriddenKeys() {
        ignoredKeys.addAll(propertiesMap.keySet());
        ignoredKeys.forEach(key -> logger.warn("{}.{} ... Ignored", this, key));
        overriddenKeys.forEach(key -> logger.warn("{}.{} ... Overridden", this, key));
    }

    private boolean alreadyExtended = false;
    // private final Set<ComposeService> extendedBy = new HashSet<>();
    private ComposeService extendFrom = null;

    // private void extendedBy(ComposeService x) {
    // if (x == this)
    // throw new ComposeFileFormatException("Service extends circular reference");
    // extendedBy.add(x);
    // }

    void afterInitialization() {
        if (extends1 != null && !alreadyExtended)
            extend();
        entrypointIsReseted = (entrypoint != null && entrypoint.list.size() == 1 && entrypoint.list.get(0).isBlank());
    }

    private void extend() {
        ComposeDocument otherComposeDocument;
        if (extends1.file == null)
            otherComposeDocument = this.composeDocument;
        else
            otherComposeDocument = this.composeDocument.getOther(extends1.file);
        extendFrom = otherComposeDocument.serviceOfName(extends1.service);
        if (extendFrom.extends1 != null && !extendFrom.alreadyExtended)
            extendFrom.extend();
        this.extend(extendFrom);
        this.alreadyExtended = true;
        // extendFrom.extendedBy(this);
    }

    private void extend(ComposeService other) {
        build = extend(build, other.build, Build::new);
        command = extend(command, other.command, Command::new);
        dependsOn = extend(dependsOn, other.dependsOn, DependsOn::new);
        entrypoint = extend(entrypoint, other.entrypoint, Entrypoint::new);
        environment = extend(environment, other.environment, Environment::new);
        envFile = extend(envFile, other.envFile, EnvFile::new);
        expose = extend(expose, other.expose, Expose::new);
        groupAdd = extend(groupAdd, other.groupAdd, GroupAdd::new);
        healthcheck = extend(healthcheck, other.healthcheck, Healthcheck::new);
        image = extend(image, other.image, Image::new);
        labels = extend(labels, other.labels, Labels::new);
        // profiles = extend(profiles, other.profiles, Profiles::new);
        // scale = extend(scale, other.scale, Scale::new);
        // user = extend(user, other.user, User::new);
        volumes = extend(volumes, other.volumes, Volumes::new);
        workingDir = extend(workingDir, other.workingDir, WorkingDir::new);
    }

    private <T> T extend(T thisProperty, T otherProperty, BinaryOperator<T> constructor) {
        if (otherProperty == null)
            return thisProperty;
        else
            return constructor.apply(thisProperty, otherProperty);
    }

    public void logInfo() {
        info(Build.KEY, build);
        info(Command.KEY, command);
        info(DependsOn.KEY, dependsOn);

        if (entrypointIsReseted)
            logger.info("{}.{}: \"\" (reseted)", this, Entrypoint.KEY);
        else
            info(Entrypoint.KEY, entrypoint);

        info(Environment.KEY, environment);
        info(EnvFile.KEY, envFile);
        info(Expose.KEY, expose);
        info(Extends.KEY, extends1);
        info(GroupAdd.KEY, groupAdd);
        info(Healthcheck.KEY, healthcheck);
        info(Image.KEY, image);
        info(Labels.KEY, labels);
        info(Profiles.KEY, profiles);
        info(Scale.KEY, scale);
        info(User.KEY, user);
        info(Volumes.KEY, volumes);
        info(WorkingDir.KEY, workingDir);
    }

    private void info(String key, Object property) {
        if (property != null)
            logger.info("{}.{}: {}", this, key, property);
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

    public Optional<String> image() {
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

    public Optional<String> workingDir() {
        return (Optional.ofNullable(workingDir).map(w -> w.path));
    }

    @Override
    public String toString() {
        return colorizedName;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMapOfObjects(Object object) {
        return (Map<String, Object>) object;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> castToMapOfStrings(Object object) {
        return (Map<String, String>) object;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> castToMapOfMaps(Object object) {
        return (Map<String, Map<String, Object>>) object;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castToStringList(Object object) {
        return (List<String>) object;
    }

    private static List<String> convertToKeyValueList(Map<String, String> keyValueMap) {
        List<String> list = new ArrayList<>();
        keyValueMap.forEach((k, v) -> {
            if (v == null)
                list.add(k);
            else
                list.add(k + "=" + v);
        });
        return list;
    }

    private String resolvePath(String pathString) {

        Path path = Paths.get(pathString);
        // if (path.isAbsolute())
        // logger.warn("{}.{} ... Contains an absolute path: {}", this, currentKey,
        // pathString);

        Path basePath = composeDocument.resolvePathsFrom();
        Path resolvedPath = basePath.resolve(path);
        try {
            return resolvedPath.toAbsolutePath().toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new ComposeFileFormatException("Unable to resolve path: " + path, e);
        }
    }

    // TODO the property classes below should be static
    // to be more decoupled from ComposeService class
    // then move them to service_properties package

    public class Build {
        public static final String KEY = "build";
        public final String context;
        public final String dockerfile;

        private Build(Object object) {
            if (object instanceof String) {
                String c = (String) object;
                context = resolvePath(c);
                dockerfile = null;
            } else {
                Map<String, Object> map = castToMapOfObjects(object);
                String c = (String) map.remove("context");
                context = resolvePath(c);
                String d = (String) map.remove("dockerfile");
                dockerfile = resolvePath(d);
                ignoredKeys(KEY, map.keySet());
            }
        }

        private Build(Build build, Build extendFrom) {
            String c = extendFrom.context;
            String d = extendFrom.dockerfile;
            if (build != null) {
                if (build.context != null) {
                    c = build.context;
                    overriddenKeys.add(KEY + ".context");
                }
                if (build.dockerfile != null) {
                    d = build.dockerfile;
                    overriddenKeys.add(KEY + ".dockerfile");
                }
            }
            context = c;
            dockerfile = d;
        }

        @Override
        public String toString() {
            return "(context=" + context + ", dockerfile=" + dockerfile + ")";
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

        private Command(Command command, Command extendFrom) {
            list = new ArrayList<>(extendFrom.list);
            if (command != null)
                list.addAll(command.list);
            // TODO replace instead of merge?
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class DependsOn {
        public static final String KEY = "depends_on";

        public final Map<String, DependsOnCondition> serviceConditionMap = new HashMap<>();
        // TODO it should be Map<ComposeService, DependsOnCondition>

        private DependsOn(Object object) {
            if (object instanceof String) {
                serviceConditionMap.put((String) object, DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY);
            } else if (object instanceof List) {
                List<String> list = castToStringList(object);
                list.forEach(serviceName -> serviceConditionMap.put(serviceName,
                        DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY));
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

        private DependsOn(DependsOn dependsOn, DependsOn extendFrom) {
            throw new ComposeFileFormatException("Unable to extend 'depends_on' property");

            // TODO it can extend
            // but only if other service is from the same document
        }

        // TODO initialize() prevent from circular dependency

        @Override
        public String toString() {
            return serviceConditionMap.toString();
        }
    }

    public class Entrypoint {
        public static final String KEY = "entrypoint";
        // public final boolean reset;
        public final List<String> list;

        private Entrypoint(Object object) {
            if (object instanceof String)
                list = Arrays.asList((String) object);
            else
                list = castToStringList(object);
            // reset = (list.size() == 1 && list.get(0).isBlank());
        }

        private Entrypoint(Entrypoint entrypoint, Entrypoint extendFrom) {
            if (entrypoint == null) {
                list = extendFrom.list;
            } else {
                overriddenKeys.add(KEY);
                list = entrypoint.list;
                // TODO merge instead of replace?
            }
            // reset = (list.size() == 1 && list.get(0).isBlank());
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class Environment {
        public static final String KEY = "environment";
        public final List<String> list;

        private Environment(Object object) {
            if (object instanceof String)
                this.list = Arrays.asList((String) object);
            else if (object instanceof Map) {
                Map<String, String> keyValueMap = castToMapOfStrings(object);
                this.list = convertToKeyValueList(keyValueMap);
            } else
                this.list = castToStringList(object);
        }

        private Environment(Environment environment, Environment extendFrom) {
            if (environment == null)
                this.list = extendFrom.list;
            else {
                this.list = new ArrayList<>();
                this.list.addAll(extendFrom.list);
                this.list.addAll(environment.list);
            }
        }

        // TODO afterConstruct
        // if variable name is duplicated, remove last ocurrences
        // warn the user
        // replicate the same pattern to other property classes

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class EnvFile {
        public static final String KEY = "env_file";
        public final List<String> list;

        private EnvFile(Object object) {
            List<String> list1;
            if (object instanceof String)
                list1 = Arrays.asList((String) object);
            else
                list1 = castToStringList(object);
            list = new ArrayList<>();
            list1.forEach(filePath -> list.add(resolvePath(filePath)));
        }

        private EnvFile(EnvFile envFile, EnvFile extendFrom) {
            list = new ArrayList<>(extendFrom.list);
            if (envFile != null) {
                list.addAll(envFile.list);
            }
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class Expose {
        public static final String KEY = "expose";
        public final List<Integer> ports;

        private Expose(Object object) {
            ports = new ArrayList<>();
        }

        private Expose(Expose expose, Expose extendFrom) {
            ports = new ArrayList<>(extendFrom.ports);
            if (expose != null) {
                ports.addAll(expose.ports);
            }
        }

        @Override
        public String toString() {
            return ports.toString();
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
                file = resolvePath((String) map.remove("file"));
                service = (String) map.remove("service");
                ignoredKeys(KEY, map.keySet());
            }
        }

        @Override
        public String toString() {
            if (file == null)
                return service;
            else
                return "(file=" + file + ", service=" + service + ")";
        }
    }

    public class GroupAdd {
        public static final String KEY = "group_add";
        public final List<String> groups;

        private GroupAdd(Object object) {
            groups = castToStringList(object);
        }

        private GroupAdd(GroupAdd groupAdd, GroupAdd extendFrom) {
            groups = new ArrayList<>(extendFrom.groups);
            if (groupAdd != null)
                groups.addAll(groupAdd.groups);
        }

        @Override
        public String toString() {
            return groups.toString();
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

        private static final String DISABLE_KEY = "disable";

        private Healthcheck(Object object) {
            Map<String, Object> map = castToMapOfObjects(object);
            if (map.containsKey(DISABLE_KEY))
                disable = (boolean) map.remove(DISABLE_KEY);
            else
                disable = false;
            test = (String) map.remove("test");
            interval = (String) map.remove("interval");
            timeout = (String) map.remove("timeout");
            retries = (String) map.remove("retries");
            startPeriod = (String) map.remove("start_period");
            ignoredKeys(KEY, map.keySet());
        }

        private Healthcheck(Healthcheck healthcheck, Healthcheck extendFrom) {
            throw new ComposeFileFormatException("Unable to extend 'healthcheck' property");
            // TODO
        }

        @Override
        public String toString() {
            return "(disable=" + disable + ", test=" + test + ", interval=" + interval + ", timeout=" + timeout
                    + ", retries=" + retries + ", start_period=" + startPeriod + ")";
        }
    }

    public class Image {
        public static final String KEY = "image";
        public final String tag;

        private Image(Object object) {
            tag = (String) object;
            // TODO validate with regex?
        }

        private Image(Image image, Image extendFrom) {
            if (image == null)
                tag = extendFrom.tag;
            else {
                tag = image.tag;
                overriddenKeys.add(KEY);
            }
        }

        @Override
        public String toString() {
            return tag;
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

        private Labels(Labels labels, Labels extendFrom) {
            list = new ArrayList<>(extendFrom.list);
            if (labels != null)
                list.addAll(labels.list);
        }

        // TODO initialize() remove last ones duplicate labes, warn the user

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class Profiles {
        public static final String KEY = "profiles";
        public final List<String> list;

        private Profiles(Object object) {
            list = castToStringList(object);
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class Scale {
        public static final String KEY = "scale";
        public final Integer number;

        private Scale(Object object) {
            number = ((Number) object).intValue();
        }

        @Override
        public String toString() {
            return number.toString();
        }
    }

    public class User {
        public static final String KEY = "user";
        public final String name;

        private User(Object object) {
            name = (String) object;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public class Volumes {
        public static final String KEY = "volumes";
        public final List<String> list;

        private Volumes(Object object) {
            List<String> tempList;
            if (object instanceof String)
                tempList = Arrays.asList((String) object);
            else
                tempList = castToStringList(object);
            list = new ArrayList<>();
            tempList.forEach(v -> {
                int i = v.indexOf(":");
                String source = v.substring(0, i);
                String target = v.substring(i + 1);
                source = resolvePath(source);
                list.add(source + ":" + target);
            });
            // TODO validate with regex?
        }

        private Volumes(Volumes volumes, Volumes extendFrom) {
            list = new ArrayList<>(extendFrom.list);
            if (volumes != null)
                list.addAll(volumes.list);
        }

        // TODO initialize()
        // remove duplicates?

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public class WorkingDir {
        public static final String KEY = "working_dir";
        public final String path;

        private WorkingDir(Object object) {
            path = (String) object;
        }

        private WorkingDir(WorkingDir workingDir, WorkingDir extendFrom) {
            if (workingDir == null)
                path = extendFrom.path;
            else {
                path = workingDir.path;
                overriddenKeys.add(KEY);
            }
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
