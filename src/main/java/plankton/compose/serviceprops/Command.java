package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class Command extends ServiceProperty<Command> {

    private List<String> lines;

    public Command() {
        super("command");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            lines = Arrays.asList((String) object);
        else
            lines = castToStringList(object);
    }

    @Override
    public Command applyTo(Command o) {
        if (o == null) {
            Command other = new Command();
            other.lines = new ArrayList<>(this.lines);
            return other;
        } else {
            return o;
        }
    }

    @Override
    public String toString() {
        return lines.toString();
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }
}
