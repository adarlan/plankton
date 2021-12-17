package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class Expose extends ServiceProperty<Expose> {

    private List<String> ports;

    public Expose() {
        super("expose");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            ports = Arrays.asList((String) object);
        else
            ports = castToStringList(object);
    }

    @Override
    public Expose applyTo(Expose o) {
        if (o == null) {
            Expose other = new Expose();
            other.ports = new ArrayList<>(this.ports);
            return other;
        } else {
            o.ports.addAll(0, this.ports);
            return o;
        }
    }

    public List<String> ports() {
        return Collections.unmodifiableList(ports);
    }

    @Override
    public String toString() {
        return ports.toString();
    }
}
