package plankton.compose.serviceprops;

import java.util.Map;

import lombok.Getter;
import plankton.compose.ServiceProperty;

public class Healthcheck extends ServiceProperty<Healthcheck> {

    @Getter
    private boolean disable;

    @Getter
    private String test;

    @Getter
    private String interval;

    @Getter
    private String timeout;

    @Getter
    private String retries;

    @Getter
    private String startPeriod;

    private static final String DISABLE_KEY = "disable";

    public Healthcheck() {
        super("healthcheck");
    }

    @Override
    public void initialize(Object object) {
        Map<String, Object> map = castToMapOfObjects(object);
        if (map.containsKey(DISABLE_KEY))
            disable = (boolean) map.remove(DISABLE_KEY);
        else
            disable = false;
        test = (String) map.remove("test");
        interval = (String) map.remove("interval");
        timeout = (String) map.remove("timeout");
        retries = (String) map.remove("retries");
        startPeriod = (String) map.remove("start_period");
        ignoredKeys.addAll(map.keySet());
    }

    @Override
    public Healthcheck applyTo(Healthcheck o) {
        if (o == null) {
            Healthcheck other = new Healthcheck();
            other.disable = this.disable;
            other.test = this.test;
            other.interval = this.interval;
            other.timeout = this.timeout;
            other.retries = this.retries;
            other.startPeriod = this.startPeriod;
            return other;
        } else
            return o;
    }

    @Override
    public String toString() {
        return "(disable=" + disable + ", test=" + test + ", interval=" + interval + ", timeout=" + timeout
                + ", retries=" + retries + ", start_period=" + startPeriod + ")";
    }
}
