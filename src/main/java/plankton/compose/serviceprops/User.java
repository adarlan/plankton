package plankton.compose.serviceprops;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class User extends ServiceProperty<User> {

    @Getter
    private String name;

    public User() {
        super("user");
    }

    @Override
    public void initialize(Object object) {
        name = (String) object;
    }

    @Override
    public User applyTo(User other) {
        if (other == null) {
            other = new User();
            other.name = this.name;
        }
        return other;
    }

    @Override
    public String toString() {
        return name;
    }
}
