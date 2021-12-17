package plankton.compose.serviceprops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import plankton.compose.ServiceProperty;

public class GroupAdd extends ServiceProperty<GroupAdd> {

    private List<String> list;

    public GroupAdd() {
        super("group_add");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String)
            list = Arrays.asList((String) object);
        else
            list = castToStringList(object);
    }

    @Override
    public GroupAdd applyTo(GroupAdd o) {
        if (o == null) {
            GroupAdd other = new GroupAdd();
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
