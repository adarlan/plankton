package plankton.compose.serviceprops;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Scale extends ServiceProperty<Scale> {

    @Getter
    private Integer number;

    public Scale() {
        super("scale");
    }

    @Override
    public void initialize(Object object) {
        number = ((Number) object).intValue();
    }

    @Override
    public Scale applyTo(Scale other) {
        if (other == null) {
            other = new Scale();
            other.number = this.number;
        }
        return other;
    }

    @Override
    public String toString() {
        return number.toString();
    }
}
