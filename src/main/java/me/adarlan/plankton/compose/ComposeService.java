package me.adarlan.plankton.compose;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComposeService {

    public final String name;
    private final Map<String, Object> map;
    private final Set<String> supportedKeys = new HashSet<>();

    public final Build build;
    public final Command command;
    public final DependsOn dependsOn;
    public final Entrypoint entrypoint;
    public final Environment environment;
    public final EnvFile envFile;
    public final Expose expose;
    public final Extends extends1;
    public final GroupAdd groupAdd;
    public final Healthcheck healthcheck;
    public final Image image;
    public final Labels labels;
    public final Profiles profiles;
    public final Scale scale;
    public final User user;
    public final Volumes volumes;
    public final VolumesFrom volumesFrom;
    public final WorkingDir workingDir;

    private static final Logger logger = LoggerFactory.getLogger(ComposeService.class);
    private final String logPrefix;

    ComposeService(String name, Object object) {

        this.name = name;

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
            if (!supportedKeys.contains(key))
                logger.warn("{}Ignoring: {}", logPrefix, key);
        });
    }

    private <T> T get(String key, Function<Object, T> function) {
        supportedKeys.add(key);
        if (map.containsKey(key)) {
            Object object = map.get(key);
            return function.apply(object);
        } else {
            return null;
        }
    }

    public class Build {
        public static final String KEY = "build";

        private Build(Object object) {
            if (isString(object)) {
                context = castToString(object);
                dockerfile = null;
            } else {
                context = getStringProperty(object, "context");
                dockerfile = getStringProperty(object, "dockerfile");
            }
            context = expandPath(context);
            if (dockerfile == null) {
                dockerfile = concatPath(context, "Dockerfile");
            } else {
                dockerfile = expandPath(dockerfile);
            }
            logger.info("{}{}.context={}", logPrefix, KEY, context);
            logger.info("{}{}.dockerfile={}", logPrefix, KEY, dockerfile);
        }

        private String context;
        private String dockerfile;

        public String context() {
            return context;
        }

        public String dockerfile() {
            return dockerfile;
        }
    }

    public class Command {
        public static final String KEY = "command";

        private Command(Object object) {
            if (isString(object)) {
                value = castToString(object);
            } else {
                List<String> list = castToStringList(object);
                value = list.stream().collect(Collectors.joining(" "));
                // TODO just join the strings?
            }
            logger.info("{}{}={}", logPrefix, KEY, value);
        }

        private String value;

        public String get() {
            return value;
        }
    }

    public class DependsOn {
        public static final String KEY = "depends_on";

        private DependsOn(Object object) {

        }

        private Map<String, String> serviceConditionMap;
    }

    public class Entrypoint {
        public static final String KEY = "entrypoint";

        private Entrypoint(Object object) {

        }

        private String value;
    }

    public class Environment {
        public static final String KEY = "environment";

        private Environment(Object object) {

        }

        private Map<String, String> variableValueMap;
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

        }

        private String url;
    }

    public class Labels {
        public static final String KEY = "labels";

        private Labels(Object object) {

        }

        private Map<String, String> nameValueMap;
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

        }

        private Integer number;
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

        private List<String> list;
    }

    public class WorkingDir {
        public static final String KEY = "working_dir";

        private WorkingDir(Object object) {

        }

        private String value;
    }

    /* ---------------------------------------------------- */

    private boolean isString(Object object) {
        return object instanceof String;
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
