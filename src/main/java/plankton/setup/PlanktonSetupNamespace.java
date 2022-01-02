package plankton.setup;

import java.time.Instant;

import lombok.Getter;

public class PlanktonSetupNamespace {

    @Getter
    private final String namespace;

    public PlanktonSetupNamespace() {
        int min = 1;
        int max = 1000000000;
        double r = (Math.random() * ((max - min) + 1)) + min;
        String a = String.valueOf((int) r);
        String b = String.valueOf(Instant.now().getEpochSecond()).trim();
        namespace = a + b;
    }
}
