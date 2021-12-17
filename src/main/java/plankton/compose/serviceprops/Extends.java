package plankton.compose.serviceprops;

import java.util.Map;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Extends extends ServiceProperty<Extends> {

    @Getter
    private String file;

    @Getter
    private String service;

    public Extends() {
        super("extends");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String) {
            file = null;
            service = (String) object;
        } else {
            Map<String, Object> map = castToMapOfObjects(object);
            file = resolvePath((String) map.remove("file"));
            service = (String) map.remove("service");
            ignoredKeys.addAll(map.keySet());
        }
    }

    @Override
    public Extends applyTo(Extends other) {
        return other;
    }

    @Override
    public String toString() {
        if (file == null)
            return service;
        else
            return "(file=" + file + ", service=" + service + ")";
    }
}
