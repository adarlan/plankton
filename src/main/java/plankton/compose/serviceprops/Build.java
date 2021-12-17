package plankton.compose.serviceprops;

import java.util.Map;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Build extends ServiceProperty<Build> {

    @Getter
    private String context = null;

    @Getter
    private String dockerfile = null;

    public Build() {
        super("build");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String) {
            String c = (String) object;
            context = resolvePath(c);
        } else {
            Map<String, Object> map = castToMapOfObjects(object);
            String c = (String) map.remove("context");
            context = resolvePath(c);
            String d = (String) map.remove("dockerfile");
            dockerfile = resolvePath(d);
            ignoredKeys.addAll(map.keySet());
        }
    }

    @Override
    public Build applyTo(Build o) {
        if (o == null) {
            Build other = new Build();
            other.context = this.context;
            other.dockerfile = this.dockerfile;
            return other;
        } else {
            return o;
        }
    }

    @Override
    public String toString() {
        return "(context=" + context + ", dockerfile=" + dockerfile + ")";
    }
}
