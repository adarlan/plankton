package plankton.compose.serviceprops;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Image extends ServiceProperty<Image> {

    @Getter
    private String tag;

    public Image() {
        super("image");
    }

    @Override
    public void initialize(Object object) {
        tag = (String) object;
    }

    @Override
    public Image applyTo(Image other) {
        if (other == null) {
            other = new Image();
            other.tag = this.tag;
        }
        return other;
    }

    @Override
    public String toString() {
        return tag;
    }
}
