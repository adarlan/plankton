package me.adarlan.plankton.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComposeService {

    private final String name;
    private final Map<String, Object> map;
    private final Set<String> supportedKeys = new HashSet<>();

    private final Build build;
    private final Command command;
    private final DependsOn dependsOn;
    private final Entrypoint entrypoint;
    private final Environment environment;
    private final EnvFile envFile;
    private final Expose expose;
    private final Extends extends1;
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

    private static final Logger logger = LoggerFactory.getLogger(ComposeService.class);
    private final String logPrefix;
    private static final String PREFIX_KEY_VALUE = "{}{}={}";

    ComposeService(String name, Object object) {

        this.name = name;
        // TODO validate name

        logPrefix = "Loading " + getClass().getSimpleName() + ": " + name + " ... ";
        logger.info(logPrefix);

        this.map = castToMap(object);

        this.build = get(Build.KEY, Build::new);
        this.command = get(Command.KEY, Command::new);
        this.dependsOn = get(DependsOn.KEY, DependsOn::new);
        this.entrypoint = get(Entrypoint.KEY, Entrypoint::new);
        this.environment = get(Environment.KEY, Environment::new);
        this.envFile = get(EnvFile.KEY, EnvFile::new);
        this.expose = get(Expose.KEY, Expose::new);
        this.extends1 = get(Extends.KEY, Extends::new);
        this.groupAdd = get(GroupAdd.KEY, GroupAdd::new);
        this.healthcheck = get(Healthcheck.KEY, Healthcheck::new);
        this.image = get(Image.KEY, Image::new);
        this.labels = get(Labels.KEY, Labels::new);
        this.profiles = get(Profiles.KEY, Profiles::new);
        this.scale = get(Scale.KEY, Scale::new);
        this.user = get(User.KEY, User::new);
        this.volumes = get(Volumes.KEY, Volumes::new);
        this.volumesFrom = get(VolumesFrom.KEY, VolumesFrom::new);
        this.workingDir = get(WorkingDir.KEY, WorkingDir::new);

        map.keySet().forEach(key -> {
            if (!supportedKeys.contains(key)) {
                logger.warn("{}Ignoring: {}", logPrefix, key);
                map.remove(key);
            }
        });
    }

    // TODO process extends1
    // TODO process volumesFrom

    private <T> T get(String key, Function<Object, T> function) {
        supportedKeys.add(key);
        if (map.containsKey(key)) {
            Object object = map.get(key);
            return function.apply(object);
        } else {
            return null;
        }
    }

    public String name() {
        return name;
    }

    public Optional<Build> build() {
        return Optional.ofNullable(build);
    }

    public Optional<String> command() {
        return Optional.ofNullable(command).map(c -> c.string);
    }

    public Map<String, String> dependsOn() {
        return Collections.unmodifiableMap(dependsOn == null ? new HashMap<>() : dependsOn.serviceConditionMap);
    }

    public Optional<String> entrypoint() {
        return Optional.ofNullable(entrypoint).map(e -> e.string);
    }

    public List<String> environment() {
        return Collections.unmodifiableList(environment == null ? new ArrayList<>() : toKeyValueList(environment.map));
    }

    public List<String> envFile() {
        return Collections.unmodifiableList(envFile == null ? new ArrayList<>() : envFile.list);
    }

    public List<Integer> expose() {
        return Collections.unmodifiableList(expose == null ? new ArrayList<>() : expose.ports);
    }

    public List<String> groupAdd() {
        return Collections.unmodifiableList(groupAdd == null ? new ArrayList<>() : groupAdd.groups);
    }

    public Optional<Healthcheck> healthcheck() {
        return Optional.ofNullable(healthcheck);
    }

    public Optional<String> image() {
        return Optional.ofNullable(image).map(i -> i.string);
    }

    public Map<String, String> labels() {
        return Collections.unmodifiableMap(labels == null ? new HashMap<>() : labels.map);
    }

    public List<String> profiles() {
        return Collections.unmodifiableList(profiles == null ? new ArrayList<>() : profiles.list);
    }

    public Integer scale() {
        return scale == null ? 1 : scale.number;
    }

    public Optional<String> user() {
        return Optional.ofNullable(user).map(u -> u.name);
    }

    public List<String> volumes() {
        return Collections.unmodifiableList(volumes == null ? new ArrayList<>() : volumes.list);
    }

    public Optional<String> workingDir() {
        return Optional.ofNullable(workingDir).map(w -> w.path);
    }

    // ------------------------------------------------------------------------

    public class Build {
        public static final String KEY = "build";

        private Build(Object object) {
            String c;
            String d = null;
            if (isString(object)) {
                c = castToString(object);
            } else {
                c = getStringProperty(object, "context");
                d = getStringProperty(object, "dockerfile");
            }
            context = expandPath(c);
            if (d == null) {
                dockerfile = concatPath(context, "Dockerfile");
            } else {
                dockerfile = expandPath(d);
            }
            logger.info("{}{}.context={}", logPrefix, KEY, context);
            logger.info("{}{}.dockerfile={}", logPrefix, KEY, dockerfile);
        }

        public final String context;
        public final String dockerfile;
    }

    public class Command {
        public static final String KEY = "command";

        private Command(Object object) {
            if (isString(object)) {
                string = castToString(object);
            } else {
                List<String> list = castToStringList(object);
                string = list.stream().collect(Collectors.joining(" "));
                // TODO just join the strings?
            }
            logger.info(PREFIX_KEY_VALUE, logPrefix, KEY, string);
        }

        public final String string;
    }

    public class DependsOn {
        public static final String KEY = "depends_on";

        public static final String SERVICE_HEALTHY = "service_healthy";
        public static final String SERVICE_STARTED = "service_started";
        public static final String SERVICE_EXITED_ZERO = "service_exited_zero";
        public static final String SERVICE_EXITED_NON_ZERO = "service_exited_non_zero";

        private DependsOn(Object object) {
            Map<String, String> m = new HashMap<>();
            if (isString(object)) {
                String s = castToString(object);
                m.put(s, SERVICE_EXITED_ZERO);
            } else if (isList(object)) {
                List<String> list = castToStringList(object);
                list.forEach(s -> m.put(s, SERVICE_EXITED_ZERO));
            } else if (isMap(object)) {
                // TODO depends_on as map
            }
            serviceConditionMap = Collections.unmodifiableMap(m);
            logger.info(PREFIX_KEY_VALUE, logPrefix, KEY, serviceConditionMap);
        }

        public final Map<String, String> serviceConditionMap;
    }

    public class Entrypoint {
        public static final String KEY = "entrypoint";

        private Entrypoint(Object object) {
            if (isString(object)) {
                string = castToString(object);
            } else {
                List<String> list = castToStringList(object);
                string = list.stream().collect(Collectors.joining(" "));
                // TODO just join the strings?
            }
            logger.info(PREFIX_KEY_VALUE, logPrefix, KEY, string);
        }

        public final String string;
    }

    public class Environment {
        public static final String KEY = "environment";

        private Environment(Object object) {

        }

        private Map<String, String> map;
    }

    public class EnvFile {
        public static final String KEY = "env_file";

        private EnvFile(Object object) {

        }

        private List<String> list;
    }

    public class Expose {
        public static final String KEY = "expose";

        private Expose(Object object) {

        }

        private List<Integer> ports;
    }

    public class Extends {
        public static final String KEY = "extends";

        private Extends(Object object) {

        }

        private String file;
        private String service;
    }

    public class GroupAdd {
        public static final String KEY = "group_add";

        private GroupAdd(Object object) {

        }

        private List<String> groups;
    }

    public class Healthcheck {
        public static final String KEY = "healthcheck";

        private Healthcheck(Object object) {

        }

        private boolean disable;
        private String test;
        private String interval;
        private String timeout;
        private String retries;
        private String startPeriod;
    }

    public class Image {
        public static final String KEY = "image";

        private Image(Object object) {
            string = castToString(object);
            logger.info(PREFIX_KEY_VALUE, logPrefix, KEY, string);
        }

        public final String string;
    }

    public class Labels {
        public static final String KEY = "labels";

        private Labels(Object object) {

        }

        private Map<String, String> map;
    }

    public class Profiles {
        public static final String KEY = "profiles";

        private Profiles(Object object) {

        }

        private List<String> list;
    }

    public class Scale {
        public static final String KEY = "scale";

        private Scale(Object object) {
            Number n = castToNumber(object);
            number = n.intValue();
            logger.info(PREFIX_KEY_VALUE, logPrefix, KEY, number);
        }

        public final Integer number;
    }

    public class User {
        public static final String KEY = "user";

        private User(Object object) {

        }

        private String name;
    }

    public class Volumes {
        public static final String KEY = "volumes";

        private Volumes(Object object) {

        }

        private List<String> list;
    }

    public class VolumesFrom {
        public static final String KEY = "volumes_from";

        private VolumesFrom(Object object) {

        }

        private List<String> stringList;
    }

    public class WorkingDir {
        public static final String KEY = "working_dir";

        private WorkingDir(Object object) {

        }

        private String path;
    }

    /* ---------------------------------------------------- */

    private boolean isString(Object object) {
        return object instanceof String;
    }

    private boolean isList(Object object) {
        return object instanceof List;
    }

    private boolean isMap(Object object) {
        return object instanceof Map;
    }

    private String castToString(Object object) {
        return (String) object;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object object) {
        return (Map<String, Object>) object;
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object object) {
        return (List<String>) object;
    }

    private Number castToNumber(Object object) {
        return (Number) object;
    }

    private List<String> toKeyValueList(Map<String, String> map) {
        List<String> list = new ArrayList<>();
        map.forEach((k, v) -> list.add(v == null ? k : (k + "=" + v)));
        return list;
    }

    private String getStringProperty(Object object, String property) {
        Map<String, Object> map = castToMap(object);
        Object o = map.get(property);
        return castToString(o);
    }

    private String expandPath(String path) {
        return path;
        // TODO
    }

    private String concatPath(String path, String subpath) {
        return path + "/" + subpath;
        // TODO
    }
}
