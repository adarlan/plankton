package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class Profiles extends ServiceProperty<Profiles> {

    private List<String> list;

    public Profiles() {
        super("profiles");
    }

    @Override
    public void initialize(Object object) {
        list = castToStringList(object);
    }

    @Override
    public Profiles applyTo(Profiles other) {
        if (other == null) {
            other = new Profiles();
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
