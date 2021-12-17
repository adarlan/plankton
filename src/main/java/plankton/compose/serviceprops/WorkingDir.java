package plankton.compose.serviceprops;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class WorkingDir extends ServiceProperty<WorkingDir> {

    @Getter
    private String path;

    public WorkingDir() {
        super("working_dir");
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public void initialize(Object object) {
        path = (String) object;
    }

    @Override
    public WorkingDir applyTo(WorkingDir other) {
        if (other == null) {
            other = new WorkingDir();
            other.path = this.path;
        }
        return other;
    }
}
