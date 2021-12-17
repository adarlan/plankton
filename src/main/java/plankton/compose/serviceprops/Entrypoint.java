package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Entrypoint extends ServiceProperty<Entrypoint> {

    private List<String> lines;

    @Getter
    private boolean reseted;

    public Entrypoint() {
        super("entrypoint");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            lines = Arrays.asList((String) object);
        else
            lines = castToStringList(object);
        reseted = (lines.size() == 1 && lines.get(0).isBlank());
    }

    @Override
    public Entrypoint applyTo(Entrypoint o) {
        if (o == null) {
            Entrypoint other = new Entrypoint();
            other.lines = new ArrayList<>(this.lines);
            other.reseted = this.reseted;
            return other;
        } else {
            return o;
        }
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public String toString() {
        return lines.toString();
    }
}
