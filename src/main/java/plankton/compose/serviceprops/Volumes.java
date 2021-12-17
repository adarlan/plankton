package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class Volumes extends ServiceProperty<Volumes> {

    private List<String> list;

    public Volumes() {
        super("volumes");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            list = Arrays.asList((String) object);
        else
            list = castToStringList(object);
        list.replaceAll(v -> {
            int i = v.indexOf(":");
            String source = v.substring(0, i);
            String target = v.substring(i + 1);
            source = resolvePath(source);
            return source + ":" + target;
        });
    }

    @Override
    public Volumes applyTo(Volumes other) {
        if (other == null) {
            other = new Volumes();
            other.list = new ArrayList<>();
        }
        other.list.addAll(0, this.list);
        return other;
    }

    public List<String> list() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
