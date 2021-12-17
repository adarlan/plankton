package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import plankton.compose.ServiceProperty;

public class Environment extends ServiceProperty<Environment> {

    private List<String> list;

    public Environment() {
        super("environment");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            list = Arrays.asList((String) object);
        else if (object instanceof Map) {
            Map<String, String> keyValueMap = castToMapOfStrings(object);
            list = convertToKeyValueList(keyValueMap);
        } else
            list = castToStringList(object);
    }

    @Override
    public Environment applyTo(Environment o) {
        if (o == null) {
            Environment other = new Environment();
            other.list = new ArrayList<>(this.list);
            return other;
        } else {
            o.list.addAll(0, this.list);
            return o;
        }
    }

    public List<String> list() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
