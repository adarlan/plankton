package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class EnvFile extends ServiceProperty<EnvFile> {

    private List<String> list;

    public EnvFile() {
        super("env_file");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            list = Arrays.asList((String) object);
        else
            list = castToStringList(object);
        list.replaceAll(this::resolvePath);
    }

    @Override
    public EnvFile applyTo(EnvFile o) {
        if (o == null) {
            EnvFile other = new EnvFile();
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
