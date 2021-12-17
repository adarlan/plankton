package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import plankton.compose.ServiceProperty;

public class Labels extends ServiceProperty<Labels> {

    private List<String> list;

    public Labels() {
        super("labels");
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
    public Labels applyTo(Labels other) {
        if (other == null) {
            other = new Labels();
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
